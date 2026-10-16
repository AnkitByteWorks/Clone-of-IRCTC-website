package com.irctc.railconnect;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	public KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
	}

	@Bean
	@ServiceConnection
	public MongoDBContainer mongoDbContainer() {
		return new MongoDBContainer(DockerImageName.parse("mongo:latest"));
	}

	@Bean
	@ServiceConnection
	public MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:latest"));
	}

	@Bean
	@ServiceConnection(name = "redis")
	public GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
	}

}
