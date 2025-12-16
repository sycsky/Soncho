package com.example.aikef.repository;

import com.example.aikef.model.OfficialChannelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfficialChannelConfigRepository extends JpaRepository<OfficialChannelConfig, UUID> {

    /**
     * 根据渠道类型查找配置
     */
    Optional<OfficialChannelConfig> findByChannelType(OfficialChannelConfig.ChannelType channelType);

    /**
     * 根据渠道类型和启用状态查找配置
     */
    Optional<OfficialChannelConfig> findByChannelTypeAndEnabledTrue(OfficialChannelConfig.ChannelType channelType);

    /**
     * 检查渠道类型是否存在
     */
    boolean existsByChannelType(OfficialChannelConfig.ChannelType channelType);
}

