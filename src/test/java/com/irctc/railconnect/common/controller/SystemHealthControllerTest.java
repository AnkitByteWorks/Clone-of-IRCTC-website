package com.irctc.railconnect.common.controller;

import com.irctc.railconnect.common.config.SecurityConfig;
import com.irctc.railconnect.user.security.JwtAuthenticationFilter;
import com.irctc.railconnect.user.security.JwtService;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemHealthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SystemHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private KafkaAdmin kafkaAdmin;

    @Test
    @DisplayName("GET /api/v1/system/status should return UP for all components when healthy without authentication")
    void shouldReturnHealthyStatusWhenAllComponentsAreUp() throws Exception {
        // Mock MySQL
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        // Mock MongoDB
        MongoDatabase mongoDatabase = mock(MongoDatabase.class);
        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        when(mongoDatabase.runCommand(any(Document.class))).thenReturn(new Document("ok", 1.0));

        // Mock Redis
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(stringRedisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        // Mock Kafka
        when(kafkaAdmin.clusterId()).thenReturn("mock-kafka-cluster-id");

        mockMvc.perform(get("/api/v1/system/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("RailConnect infrastructure status"))
                .andExpect(jsonPath("$.data.mysql").value("UP"))
                .andExpect(jsonPath("$.data.mongodb").value("UP"))
                .andExpect(jsonPath("$.data.redis").value("UP"))
                .andExpect(jsonPath("$.data.kafka").value("UP"))
                .andExpect(jsonPath("$.data.javaVersion").isNotEmpty())
                .andExpect(jsonPath("$.data.virtualThreadsEnabled").isBoolean());
    }

    @Test
    @DisplayName("GET /api/v1/system/status should report DOWN for failing components without crashing")
    void shouldHandleDownComponentsGracefully() throws Exception {
        // Simulate MySQL down and Kafka down
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class)))
                .thenThrow(new RuntimeException("Connection refused"));
        when(kafkaAdmin.clusterId()).thenThrow(new RuntimeException("Broker not available"));

        // Mongo and Redis UP
        MongoDatabase mongoDatabase = mock(MongoDatabase.class);
        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(stringRedisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        mockMvc.perform(get("/api/v1/system/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mysql").value("DOWN: Connection refused"))
                .andExpect(jsonPath("$.data.kafka").value("DOWN: Broker not available"))
                .andExpect(jsonPath("$.data.mongodb").value("UP"))
                .andExpect(jsonPath("$.data.redis").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/1 should require authentication based on SecurityConfig")
    void shouldRequireAuthenticationForProtectedRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/1"))
                .andExpect(status().isUnauthorized());
    }
}
