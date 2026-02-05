package com.example.aikef.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private String port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean ssl;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String protocol = ssl ? "rediss://" : "redis://";
        // Ensure host doesn't already contain protocol
        String cleanHost = host.replace("redis://", "").replace("rediss://", "").replace("http://", "").replace("https://", "");
        String address = protocol + cleanHost + ":" + port;
        
        // Single Server Mode
        config.useSingleServer()
              .setAddress(address)
              .setDatabase(database);
        
        // Only set password if it is not empty
        if (StringUtils.hasText(password)) {
            config.useSingleServer().setPassword(password);
        }

        return Redisson.create(config);
    }
}
