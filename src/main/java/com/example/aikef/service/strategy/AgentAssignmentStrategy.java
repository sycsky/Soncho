package com.example.aikef.service.strategy;

import com.example.aikef.model.Agent;
import com.example.aikef.model.Channel;
import com.example.aikef.model.Customer;

import java.util.List;

/**
 * 客服分配策略抽象类
 * 支持自定义扩展不同的分配逻辑
 */
public abstract class AgentAssignmentStrategy {

    /**
     * 为客户分配主责客服
     *
     * @param customer 客户信息
     * @param channel 渠道
     * @param group 群组（可选）
     * @return 分配的主责客服
     */
    public abstract Agent assignPrimaryAgent(Customer customer, Channel channel);

    /**
     * 为客户分配支持客服（可选）
     *
     * @param customer 客户信息
     * @param channel 渠道
     * @param primaryAgent 主责客服
     * @param group 群组
     * @return 支持客服列表
     */
    public List<Agent> assignSupportAgents(Customer customer, Channel channel, Agent primaryAgent) {
        // 默认不分配支持客服
        return List.of();
    }

    /**
     * 获取策略名称
     */
    public abstract String getStrategyName();
}
