package com.irctc.railconnect.common.controller;

import com.irctc.railconnect.common.response.ApiResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final JdbcTemplate jdbcTemplate;
    private final MongoTemplate mongoTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaAdmin kafkaAdmin;

    public SystemHealthController(
            JdbcTemplate jdbcTemplate,
            MongoTemplate mongoTemplate,
            StringRedisTemplate stringRedisTemplate,
            KafkaAdmin kafkaAdmin
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mongoTemplate = mongoTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.kafkaAdmin = kafkaAdmin;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getSystemStatus() {
        Map<String, Object> statusMap = new LinkedHashMap<>();
        statusMap.put("javaVersion", System.getProperty("java.version"));
        statusMap.put("virtualThreadsEnabled", Thread.currentThread().isVirtual());
        statusMap.put("mysql", checkMysql());
        statusMap.put("mongodb", checkMongodb());
        statusMap.put("redis", checkRedis());
        statusMap.put("kafka", checkKafka());

        return ApiResponse.ok("RailConnect infrastructure status", statusMap);
    }

    private String checkMysql() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return (result != null && result == 1) ? "UP" : "DOWN: Unexpected query result";
        } catch (Exception ex) {
            return "DOWN: " + ex.getMessage();
        }
    }

    private String checkMongodb() {
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
            return "UP";
        } catch (Exception ex) {
            return "DOWN: " + ex.getMessage();
        }
    }

    private String checkRedis() {
        try {
            RedisConnectionFactory factory = stringRedisTemplate.getConnectionFactory();
            if (factory == null) {
                return "DOWN: Redis connection factory is null";
            }
            try (RedisConnection connection = factory.getConnection()) {
                String ping = connection.ping();
                return ("PONG".equalsIgnoreCase(ping) || ping != null) ? "UP" : "DOWN: Unexpected ping response";
            }
        } catch (Exception ex) {
            return "DOWN: " + ex.getMessage();
        }
    }

    private String checkKafka() {
        try {
            String clusterId = kafkaAdmin.clusterId();
            if (clusterId != null && !clusterId.isBlank()) {
                return "UP";
            }
            Map<String, Object> configs = kafkaAdmin.getConfigurationProperties();
            if (configs == null || configs.isEmpty()) {
                return "DOWN: KafkaAdmin configuration properties are missing";
            }
            try (AdminClient adminClient = AdminClient.create(configs)) {
                adminClient.describeCluster().clusterId().get(2, TimeUnit.SECONDS);
                return "UP";
            }
        } catch (Exception ex) {
            return "DOWN: " + ex.getMessage();
        }
    }
}
