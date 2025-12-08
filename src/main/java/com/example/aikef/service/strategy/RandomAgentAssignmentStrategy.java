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
        // 查询所有在线客服
        List<Agent> onlineAgents = agentRepository.findByStatus(AgentStatus.ONLINE);
        
        if (onlineAgents.isEmpty()) {
            // 如果没有在线客服，查询所有活跃客服
            onlineAgents = agentRepository.findAll().stream()
                    .filter(agent -> agent.getStatus() != AgentStatus.OFFLINE)
                    .toList();
        }
        
        if (onlineAgents.isEmpty()) {
            log.warn("没有可用的客服进行分配");
            throw new IllegalStateException("当前没有可用的客服");
        }
        
        // 随机选择一个客服
        Agent selectedAgent = onlineAgents.get(random.nextInt(onlineAgents.size()));
        log.info("为客户 {} 分配客服: {} (策略: 随机分配)", customer.getName(), selectedAgent.getName());
        
        return selectedAgent;
    }

    @Override
    public String getStrategyName() {
        return "RANDOM";
    }
}
