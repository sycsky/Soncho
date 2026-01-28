package com.example.aikef.service.strategy;

import com.example.aikef.model.Agent;
import com.example.aikef.model.Channel;
import com.example.aikef.model.Customer;
import com.example.aikef.model.enums.AgentStatus;
import com.example.aikef.repository.AgentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 随机分配策略
 * 从在线的客服中随机选择一个
 */
@Component
public class RandomAgentAssignmentStrategy extends AgentAssignmentStrategy {

    private static final Logger log = LoggerFactory.getLogger(RandomAgentAssignmentStrategy.class);
    private final AgentRepository agentRepository;
    private final Random random = new Random();

    public RandomAgentAssignmentStrategy(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    public Agent assignPrimaryAgent(Customer customer, Channel channel) {
        // 1. 优先分配在线客服 (ONLINE)
        List<Agent> candidates = agentRepository.findByStatus(AgentStatus.ONLINE);
        
        // 2. 如果没有在线客服，分配忙碌客服 (BUSY)
        if (candidates.isEmpty()) {
            candidates = agentRepository.findByStatus(AgentStatus.BUSY);
        }
        
        // 3. 如果也没有忙碌客服，分配离线客服 (OFFLINE)
        if (candidates.isEmpty()) {
             candidates = agentRepository.findByStatus(AgentStatus.OFFLINE);
        }

        // 4. 兜底：如果上述都为空（可能状态值不匹配或无数据），获取所有客服
        if (candidates.isEmpty()) {
             candidates = agentRepository.findAll();
        }
        
        if (candidates.isEmpty()) {
            log.warn("系统没有任何客服数据，无法进行分配");
            throw new IllegalStateException("当前没有可用的客服");
        }
        
        // 随机选择一个客服
        Agent selectedAgent = candidates.get(random.nextInt(candidates.size()));
        log.info("为客户 {} 分配客服: {} (状态: {}, 策略: 随机分配)", 
                customer.getName(), selectedAgent.getName(), selectedAgent.getStatus());
        
        return selectedAgent;
    }

    @Override
    public String getStrategyName() {
        return "RANDOM";
    }
}
