package com.example.aikef.service;

import com.example.aikef.dto.ScheduledTaskDto;
import com.example.aikef.dto.request.SaveScheduledTaskRequest;
import com.example.aikef.model.*;
import com.example.aikef.model.enums.TaskCustomerMode;
import com.example.aikef.repository.*;
import com.example.aikef.workflow.service.AiWorkflowService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class AiScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(AiScheduledTaskService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // 限制并发数的线程池，防止大量任务同时触发耗尽资源
    // 最小2线程，最大为 CPU 核心数
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));

    @Resource
    private AiScheduledTaskRepository taskRepository;

    @Resource
    private AiWorkflowRepository workflowRepository;

    @Resource
    private CustomerRepository customerRepository;

    @Resource
    private SpecialCustomerRepository specialCustomerRepository;

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @Resource
    private AiWorkflowService workflowService;

    @Resource
    private ObjectMapper objectMapper;

    @Autowired
    private SessionMessageGateway sessionMessageGateway;


    @Transactional
    public ScheduledTaskDto createTask(SaveScheduledTaskRequest request) {
        AiScheduledTask task = new AiScheduledTask();
        updateTaskFromRequest(task, request);
        
        // 计算首次运行时间
        updateNextRunTime(task);
        
        task = taskRepository.save(task);
        return convertToDto(task);
    }

    @Transactional
    public ScheduledTaskDto updateTask(UUID id, SaveScheduledTaskRequest request) {
        AiScheduledTask task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("定时任务不存在"));
        
        updateTaskFromRequest(task, request);
        updateNextRunTime(task);
        
        task = taskRepository.save(task);
        return convertToDto(task);
    }

    @Transactional
    public void deleteTask(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new EntityNotFoundException("定时任务不存在");
        }
        taskRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ScheduledTaskDto getTask(UUID id) {
        return taskRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new EntityNotFoundException("定时任务不存在"));
    }

    @Transactional(readOnly = true)
    public List<ScheduledTaskDto> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 定时调度器，每分钟检查一次
     */
    @Scheduled(fixedDelay = 60000)
    public void scheduleTask() {
        Instant now = Instant.now();
        List<AiScheduledTask> dueTasks = taskRepository.findByEnabledTrueAndNextRunAtLessThanEqual(now);
        
        for (AiScheduledTask task : dueTasks) {
            try {
                executeTask(task);
            } catch (Exception e) {
                log.error("执行定时任务失败: taskId={}, name={}", task.getId(), task.getName(), e);
            } finally {
                // 更新下次运行时间
                try {
                    updateNextRunTime(task);
                    task.setLastRunAt(now);
                    taskRepository.save(task);
                } catch (Exception e) {
                    log.error("更新任务下次运行时间失败: taskId={}", task.getId(), e);
                }
            }
        }
    }

    @Transactional
    public void executeTask(AiScheduledTask task) {
        log.info("开始执行定时任务: id={}, name={}", task.getId(), task.getName());
        
        // 1. 获取目标客户
        Set<Customer> targetCustomers = resolveTargetCustomers(task);
        if (targetCustomers.isEmpty()) {
            log.warn("定时任务没有匹配的目标客户: taskId={}", task.getId());
            return;
        }
        
        // 2. 遍历客户执行工作流
        int successCount = 0;
        for (Customer customer : targetCustomers) {
            // 异步执行每个客户的工作流，使用自定义线程池限制并发
            CompletableFuture.runAsync(() -> {
                try {
                    // 查找最新会话
                    ChatSession session = chatSessionRepository.findFirstByCustomer_IdOrderByLastActiveAtDesc(customer.getId());
                    if (session == null) {
                        log.debug("客户没有活跃会话，跳过: customerId={}", customer.getId());
                        return;
                    }
                    
                    // 准备变量
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("customerId", customer.getId());
                    variables.put("customerName", customer.getName());
                    variables.put("taskId", task.getId());
                    variables.put("taskName", task.getName());
                    
                    // 启动工作流
                    AiWorkflowService.WorkflowExecutionResult result = workflowService.executeWorkflow(
                        task.getWorkflow().getId(),
                        session.getId(),
                        task.getInitialInput() != null ? task.getInitialInput() : "Scheduled Task Trigger",
                        variables
                    );

                    // 如果工作流执行成功且有回复，发送 AI 回复消息（类似用户对话工作流）
                    if (result.success() && result.reply() != null && !result.reply().isBlank()) {
                        try {
                            sessionMessageGateway.sendAiMessage(session.getId(), result.reply());
                            log.info("事件触发工作流执行成功，已发送 AI 回复: taskName={}, sessionId={}, replyLength={}",
                                    task.getName(), session.getId(), result.reply().length());
                        } catch (Exception e) {
                            log.error("发送 AI 回复失败: taskName={}, sessionId={}", task.getName(), session.getId(), e);
                            // 即使发送失败，也返回工作流执行结果
                        }
                    } else if (!result.success()) {
                        log.warn("事件触发工作流执行失败: taskName={}, sessionId={}, error={}",
                                task.getName(), session.getId(), result.errorMessage());
                    }
                    
                } catch (Exception e) {
                    log.error("为客户执行定时任务失败: customerId={}", customer.getId(), e);
                }
            }, taskExecutor);
            successCount++;
        }
        
        log.info("定时任务触发完成: taskId={}, 触发客户数={}", task.getId(), successCount);
    }

    @PreDestroy
    public void shutdown() {
        if (taskExecutor != null) {
            taskExecutor.shutdown();
        }
    }

    private void updateTaskFromRequest(AiScheduledTask task, SaveScheduledTaskRequest request) {
        task.setName(request.name());
        task.setDescription(request.description());
        task.setEnabled(request.enabled() != null ? request.enabled() : true);
        task.setCustomerMode(request.customerMode());
        task.setInitialInput(request.initialInput());
        
        // 设置工作流
        AiWorkflow workflow = workflowRepository.findById(request.workflowId())
                .orElseThrow(() -> new EntityNotFoundException("工作流不存在"));
        task.setWorkflow(workflow);
        
        // 序列化配置
        try {
            task.setScheduleConfig(objectMapper.writeValueAsString(request.scheduleConfig()));
            task.setTargetConfig(objectMapper.writeValueAsString(request.targetIds()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON序列化失败", e);
        }
        
        // 生成 Cron 表达式
        String cron = generateCronExpression(request.scheduleConfig());
        task.setCronExpression(cron);
    }
    
    private void updateNextRunTime(AiScheduledTask task) {
        if (Boolean.FALSE.equals(task.getEnabled())) {
            task.setNextRunAt(null);
            return;
        }
        
        try {
            CronExpression cron = CronExpression.parse(task.getCronExpression());
            // ZonedDateTime is safer for CronExpression in Spring 6+
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
            java.time.ZonedDateTime nextZdt = cron.next(now);
            
            if (nextZdt != null) {
                task.setNextRunAt(nextZdt.toInstant());
            } else {
                task.setNextRunAt(null);
            }
        } catch (Exception e) {
            log.error("计算下次运行时间失败: cron={}", task.getCronExpression(), e);
            task.setNextRunAt(null);
        }
    }

    private String generateCronExpression(SaveScheduledTaskRequest.ScheduleConfig config) {
        // 默认秒为0
        String second = "0";
        String minute = "0";
        String hour = "0";
        
        if (config.time() != null) {
            try {
                // 支持 HH:mm:ss 或 HH:mm
                String timeStr = config.time().length() == 5 ? config.time() + ":00" : config.time();
                LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);
                second = String.valueOf(time.getSecond());
                minute = String.valueOf(time.getMinute());
                hour = String.valueOf(time.getHour());
            } catch (Exception e) {
                log.warn("时间格式解析失败，使用默认 00:00:00: {}", config.time());
            }
        }
        
        String dayOfMonth = "*";
        String month = "*";
        String dayOfWeek = "?";
        
        switch (config.type()) {
            case "DAILY":
                dayOfMonth = "*";
                dayOfWeek = "?";
                break;
                
            case "WEEKLY":
                dayOfMonth = "?";
                if (config.daysOfWeek() != null && !config.daysOfWeek().isEmpty()) {
                    // Spring CronExpression: 0-7 (0 or 7 is SUN), or MON-SUN.
                    // User input: 1-7 (Assuming 1=Mon, 7=Sun).
                    // Let's map to MON,TUE... to be safe.
                    String[] days = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
                    dayOfWeek = config.daysOfWeek().stream()
                            .filter(d -> d >= 1 && d <= 7)
                            .map(d -> days[d - 1])
                            .collect(Collectors.joining(","));
                } else {
                    // 必须指定至少一天，否则 Spring CronExpression 解析器会报错
                    // 如果用户没选，默认周一 (或者抛错)
                    dayOfWeek = "MON"; 
                }
                break;
                
            case "MONTHLY":
                dayOfWeek = "?";
                if (config.daysOfMonth() != null && !config.daysOfMonth().isEmpty()) {
                    dayOfMonth = config.daysOfMonth().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                } else {
                    dayOfMonth = "1"; // Default to 1st
                }
                break;
                
            default:
                throw new IllegalArgumentException("不支持的调度类型: " + config.type());
        }
        
        // Cron format: second minute hour dayOfMonth month dayOfWeek
        return String.format("%s %s %s %s %s %s", second, minute, hour, dayOfMonth, month, dayOfWeek);
    }

    private Set<Customer> resolveTargetCustomers(AiScheduledTask task) {
        Set<Customer> customers = new HashSet<>();
        if (task.getTargetConfig() == null) return customers;
        
        try {
            List<String> targetIds = objectMapper.readValue(task.getTargetConfig(), new TypeReference<List<String>>() {});
            if (targetIds == null || targetIds.isEmpty()) return customers;
            
            if (task.getCustomerMode() == TaskCustomerMode.SPECIFIC_CUSTOMER) {
                List<UUID> ids = targetIds.stream().map(UUID::fromString).toList();
                customers.addAll(customerRepository.findAllById(ids));
            } else if (task.getCustomerMode() == TaskCustomerMode.CUSTOMER_ROLE) {
                // 查找具有指定角色的特殊客户
                List<UUID> roleIds = targetIds.stream().map(UUID::fromString).toList();
                List<SpecialCustomer> specials = specialCustomerRepository.findByRole_IdIn(roleIds);
                
                for (SpecialCustomer sc : specials) {
                    if (sc.getCustomer() != null) {
                        customers.add(sc.getCustomer());
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析目标客户配置失败", e);
        }
        return customers;
    }

    private ScheduledTaskDto convertToDto(AiScheduledTask task) {
        ScheduledTaskDto dto = new ScheduledTaskDto(
            task.getId(),
            task.getName(),
            task.getDescription(),
            task.getWorkflow().getId(),
            task.getWorkflow().getName(),
            parseJson(task.getScheduleConfig()),
            task.getCustomerMode(),
            parseJsonList(task.getTargetConfig()),
            task.getInitialInput(),
            task.getEnabled(),
            task.getCronExpression(),
            task.getLastRunAt(),
            task.getNextRunAt(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
        return dto;
    }
    
    private Object parseJson(String json) {
        try {
            return json != null ? objectMapper.readValue(json, Object.class) : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    private List<String> parseJsonList(String json) {
        try {
            return json != null ? objectMapper.readValue(json, new TypeReference<List<String>>() {}) : Collections.emptyList();
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
