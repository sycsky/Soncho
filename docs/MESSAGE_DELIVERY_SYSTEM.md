# 消息发送记录系统（仅客服）

## 概述

为了解决多客服场景下的离线消息推送问题，引入了**消息发送记录表 (message_delivery)**，用于追踪每条消息对每个客服的发送状态。

**重要说明**：该系统**只为客服创建发送记录**，客户端通过历史消息接口获取所有消息（包括未读消息），因此客户不需要 WebSocket 推送离线消息。

## 问题背景

### 旧方案的问题
之前使用 `Message` 表的 `readByAgent` 字段来标记消息已读状态，存在以下问题：

1. **多客服支持不足**：一个会话有主责客服和多个支持客服，无法区分每个客服的已读状态
2. **群聊场景不支持**：客服之间的消息也需要推送，但无法追踪每个客服是否已收到
3. **发送者也需要记录**：发送者发送消息时即代表已收到，但旧方案无法优雅处理

### 新方案的优势
- ✅ **精确追踪**：每条消息对每个客服创建独立的发送记录
- ✅ **发送者自动标记**：客服发送时自动标记该客服已发送
- ✅ **支持多客服**：主责和支持客服都有独立的发送记录
- ✅ **支持群聊**：客服之间的消息也能正确推送
- ✅ **客户端简化**：客户通过历史消息接口获取，无需 WebSocket 推送

## 数据库设计

### message_delivery 表结构

```sql
CREATE TABLE message_delivery (
    id CHAR(36) PRIMARY KEY,
    message_id CHAR(36) NOT NULL,        -- 消息ID
    agent_id CHAR(36) NOT NULL,          -- 接收客服ID
    customer_id CHAR(36),                -- 预留字段（当前不使用）
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,  -- 是否已发送
    sent_at TIMESTAMP NULL,              -- 发送时间
    created_at TIMESTAMP NOT NULL,       -- 创建时间
    
    INDEX idx_agent_not_sent (agent_id, is_sent),
    INDEX idx_message (message_id)
);
```

### 字段说明
- **id**: 主键
- **message_id**: 关联的消息ID
- **agent_id**: 接收客服ID（必填）
- **customer_id**: 预留字段（当前不使用，客户通过历史消息接口获取）
- **is_sent**: 是否已发送（false = 未发送，true = 已发送）
- **sent_at**: 发送时间（已发送时记录）
- **created_at**: 记录创建时间

## 核心逻辑

### 1. 消息发送时创建发送记录（仅客服）

当用户或客服发送消息时（`ConversationService.sendMessage()`）：

```java
private void createMessageDeliveries(Message message, ChatSession session, UUID senderId) {
    List<MessageDelivery> deliveries = new ArrayList<>();
    
    // 1. 为主责客服创建发送记录
    if (session.getPrimaryAgent() != null) {
        MessageDelivery delivery = new MessageDelivery();
        delivery.setMessage(message);
        delivery.setAgentId(session.getPrimaryAgent().getId());
        // 如果是发送者本人（客服发送），标记为已发送
        if (senderId.equals(session.getPrimaryAgent().getId())) {
            delivery.setSent(true);
            delivery.setSentAt(Instant.now());
        }
        deliveries.add(delivery);
    }
    
    // 2. 为支持客服创建发送记录
    for (UUID supportAgentId : session.getSupportAgentIds()) {
        MessageDelivery delivery = new MessageDelivery();
        delivery.setMessage(message);
        delivery.setAgentId(supportAgentId);
        // 如果是发送者本人（客服发送），标记为已发送
        if (senderId.equals(supportAgentId)) {
            delivery.setSent(true);
            delivery.setSentAt(Instant.now());
        }
        deliveries.add(delivery);
    }
    
    // 3. 批量保存（注意：不为客户创建发送记录）
    messageDeliveryRepository.saveAll(deliveries);
}
```

### 2. 客服上线时推送离线消息

当客服建立 WebSocket 连接时（`ChatWebSocketHandler.afterConnectionEstablished()`）：

```java
// 客服上线
if (agentPrincipal != null) {
    // 1. 查询未发送的消息
    List<ChatMessageDto> unsentMessages = 
        offlineMessageService.getUnsentMessagesForAgent(agentId);
    
    // 2. 推送消息
    for (ChatMessageDto message : unsentMessages) {
        session.sendMessage(...);
    }
    
    // 3. 标记为已发送
    offlineMessageService.markAsSentForAgent(agentId);
}

// 客户上线（不推送离线消息）
if (customerPrincipal != null) {
    // 仅注册连接，不推送离线消息
    // 客户通过历史消息接口获取所有消息
    sessionManager.registerCustomer(customerId, session);
}
```

### 3. 查询未发送消息（仅客服）

```java
// 查询客服的未发送消息
@Query("SELECT md FROM MessageDelivery md " +
       "WHERE md.agentId = :agentId AND md.isSent = false " +
       "ORDER BY md.createdAt ASC")
List<MessageDelivery> findUnsentForAgent(@Param("agentId") UUID agentId);
```

## 使用场景示例

### 场景1：客户发送消息

```
1. 客户A 在会话中发送消息
   - 会话参与者：客户A、主责客服B、支持客服C

2. 创建2条发送记录（仅客服）：
   - record1: agentId=B, isSent=false (需要推送)
   - record2: agentId=C, isSent=false (需要推送)
   
   注意：不为客户A创建发送记录

3. 客服B上线：
   - 查询未发送消息 → 找到 record1
   - 推送消息给客服B
   - 标记 record1.isSent = true

4. 客户A刷新页面：
   - 调用历史消息接口 → 直接获取所有消息（包括未读）
   - 无需 WebSocket 推送
```

### 场景2：客服发送消息（群聊）

```
1. 客服B 在会话中发送消息
   - 会话参与者：客户A、主责客服B、支持客服C

2. 创建2条发送记录（仅客服）：
   - record1: agentId=B, isSent=true (发送者本人)
   - record2: agentId=C, isSent=false (需要推送给其他客服)
   
   注意：不为客户A创建发送记录

3. 客户A打开聊天窗口：
   - 调用历史消息接口 → 直接获取所有消息
   - 无需 WebSocket 推送

4. 客服C上线：
   - 查询未发送消息 → 找到 record2
   - 推送消息给客服C
   - 标记 record2.isSent = true
```

## 客户端获取消息方式

### 客户端
- ✅ **历史消息接口**：打开聊天窗口时调用历史消息接口，获取所有消息（包括未读）
- ✅ **WebSocket 实时消息**：连接后接收新消息的实时推送
- ❌ **不使用离线消息推送**：无需在连接时推送离线消息

### 客服端
- ✅ **历史消息接口**：查看会话历史
- ✅ **WebSocket 实时消息**：接收新消息的实时推送
- ✅ **WebSocket 离线消息推送**：上线时自动推送未读消息

## 文件清单

### 新增文件
1. **MessageDelivery.java** - 消息发送记录实体
2. **MessageDeliveryRepository.java** - 消息发送记录Repository
3. **db/004_create_message_delivery.sql** - 数据库迁移脚本

### 修改文件
1. **OfflineMessageService.java** - 重构为基于发送记录的离线消息服务
2. **ConversationService.java** - 发送消息时创建发送记录
3. **ChatWebSocketHandler.java** - 推送离线消息并标记已发送

### 删除文件
1. **OfflineMessageController.java** - 离线消息现在自动推送，不需要手动API

### 性能优化

### 索引设计
```sql
-- 优化客服查询未发送消息
INDEX idx_agent_not_sent (agent_id, is_sent)

-- 优化按消息查询发送记录
INDEX idx_message (message_id)
```

### 查询效率
- 查询未发送消息：使用联合索引 `(agent_id, is_sent)`，效率高
- 批量标记已发送：一次性更新所有未发送记录

## 注意事项

1. **仅客服创建记录**：只为客服创建发送记录，客户通过历史消息接口获取消息
2. **发送者自动标记**：客服发送消息时，该客服的发送记录自动标记为 `isSent=true`
3. **WebSocket推送后标记**：只有在成功推送后才标记为已发送
4. **懒加载初始化**：查询消息时需要初始化 `attachments` 和 `mentionAgentIds`
5. **事务控制**：标记已发送操作使用 `@Transactional` 保证一致性
6. **客户端简化**：客户端不需要离线消息推送，减少系统复杂度

## 未来扩展

可以考虑添加以下功能：
- 推送失败重试机制
- 推送历史记录（保留已发送记录用于审计）
- 消息送达回执
- 客服未读消息统计（基于未发送记录）
