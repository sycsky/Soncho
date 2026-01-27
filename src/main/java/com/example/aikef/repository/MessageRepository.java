package com.example.aikef.repository;

import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.MessageType;
import com.example.aikef.model.enums.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findBySession_IdOrderByCreatedAtAsc(UUID sessionId);

    Page<Message> findBySession_IdOrderByCreatedAtAsc(UUID sessionId, Pageable pageable);

    /**
     * 查询会话消息（按创建时间倒序，最新的在前）
     */
    Page<Message> findBySession_IdOrderByCreatedAtDesc(UUID sessionId, Pageable pageable);

    /**
     * 查询客户未读消息（客服发送的消息）
     */
    List<Message> findBySessionAndReadByCustomerFalseAndSenderTypeNot(
            ChatSession session, SenderType senderType);

    /**
     * 查询客服未读消息（客户发送的消息）
     */
    List<Message> findBySessionAndReadByAgentFalseAndSenderType(
            ChatSession session, SenderType senderType);

    /**
     * 统计客户未读消息数
     */
    long countBySessionAndReadByCustomerFalseAndSenderTypeNot(
            ChatSession session, SenderType senderType);

    /**
     * 统计客服未读消息数
     */
    long countBySessionAndReadByAgentFalseAndSenderType(
            ChatSession session, SenderType senderType);

    /**
     * 查询会话的最后一条消息
     */
    Message findFirstBySession_IdOrderByCreatedAtDesc(UUID sessionId);

    /**
     * 统计会话的消息总数
     */
    long countBySession_Id(UUID sessionId);

    /**
     * 统计会话在指定时间之后的消息数
     */
    long countBySession_IdAndCreatedAtAfter(UUID sessionId, java.time.Instant after);

    /**
     * 统计会话在指定时间之后且排除特定类型的消息数
     */
    long countBySession_IdAndCreatedAtAfterAndSenderTypeNot(UUID sessionId, java.time.Instant after, SenderType senderType);

    /**
     * 统计会话中排除特定类型的消息总数
     */
    long countBySession_IdAndSenderTypeNot(UUID sessionId, SenderType senderType);

    /**
     * 查询会话的非内部消息（客户可见，按创建时间正序）
     */
    Page<Message> findBySession_IdAndInternalFalseOrderByCreatedAtAsc(UUID sessionId, Pageable pageable);

    /**
     * 查询会话的非内部消息（客户可见，按创建时间倒序，最新的在前）
     */
    Page<Message> findBySession_IdAndInternalFalseOrderByCreatedAtDesc(UUID sessionId, Pageable pageable);

    /**
     * 查询会话的最后一条指定类型的消息
     */
    Message findFirstBySession_IdAndSenderTypeOrderByCreatedAtDesc(UUID sessionId, SenderType senderType);

    /**
     * 查询会话在指定时间之后的所有消息（按时间正序）
     */
    List<Message> findBySession_IdAndCreatedAtAfterOrderByCreatedAtAsc(UUID sessionId, java.time.Instant after);

    /**
     * 查询会话在指定时间之后且不包含指定类型的消息（按时间正序）
     */
    List<Message> findBySession_IdAndCreatedAtAfterAndSenderTypeNotOrderByCreatedAtAsc(
            UUID sessionId, java.time.Instant after, SenderType senderType);

    /**
     * 查询会话在指定时间之前或等于的消息（按时间倒序，用于历史记录）
     * 用于根据触发工作流的消息时间点加载历史记录
     */
    List<Message> findBySession_IdAndInternalFalseAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
            UUID sessionId, java.time.Instant maxCreatedAt, Pageable pageable);

    long countByCreatedAtBetweenAndTenantId(java.time.Instant start, java.time.Instant end, String tenantId);

    long countByCreatedAtBetweenAndTenantIdAndSenderType(
            java.time.Instant start, java.time.Instant end, String tenantId, SenderType senderType);


    /**
     * 查询会话消息，排除指定发送者类型（按创建时间倒序）
     */
    Page<Message> findBySession_IdAndSenderTypeNotInOrderByCreatedAtDesc(
            UUID sessionId, java.util.Collection<SenderType> senderTypes, Pageable pageable);

    /**
     * 查询会话非内部消息，排除指定发送者类型（按创建时间倒序）
     */
    Page<Message> findBySession_IdAndInternalFalseAndSenderTypeNotInOrderByCreatedAtDesc(
            UUID sessionId, java.util.Collection<SenderType> senderTypes, Pageable pageable);

    long countByCreatedAtBetweenAndTenantIdAndMessageType(
            java.time.Instant start, java.time.Instant end, String tenantId, MessageType type);
}
