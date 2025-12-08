package com.example.aikef.repository;

import com.example.aikef.model.Message;
import com.example.aikef.model.MessageDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageDeliveryRepository extends JpaRepository<MessageDelivery, UUID> {

    /**
     * 查询客服未发送的消息
     */
    @Query("SELECT md FROM MessageDelivery md " +
           "WHERE md.agentId = :agentId AND md.isSent = false " +
           "ORDER BY md.createdAt ASC")
    List<MessageDelivery> findUnsentForAgent(@Param("agentId") UUID agentId);

    /**
     * 查询客户未发送的消息
     */
    @Query("SELECT md FROM MessageDelivery md " +
           "WHERE md.customerId = :customerId AND md.isSent = false " +
           "ORDER BY md.createdAt ASC")
    List<MessageDelivery> findUnsentForCustomer(@Param("customerId") UUID customerId);

    /**
     * 查询某条消息的所有发送记录
     */
    List<MessageDelivery> findByMessage(Message message);

    /**
     * 批量标记为已发送
     */
    @Query("UPDATE MessageDelivery md SET md.isSent = true, md.sentAt = CURRENT_TIMESTAMP " +
           "WHERE md.id IN :ids")
    void markAsSent(@Param("ids") List<UUID> ids);
}
