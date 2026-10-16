package com.irctc.railconnect.inventory.service;

import com.irctc.railconnect.inventory.config.InventoryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);
    private static final String LOCK_KEY_PREFIX = "inventory:lock:";

    private final StringRedisTemplate redisTemplate;
    private final InventoryProperties inventoryProperties;

    public SeatLockService(StringRedisTemplate redisTemplate, InventoryProperties inventoryProperties) {
        this.redisTemplate = redisTemplate;
        this.inventoryProperties = inventoryProperties;
    }

    public boolean acquireSeatLock(
            String trainNumber,
            LocalDate journeyDate,
            String coachNumber,
            int seatNumber,
            String userEmail
    ) {
        String key = buildLockKey(trainNumber, journeyDate, coachNumber, seatNumber);
        Duration ttl = Duration.ofSeconds(inventoryProperties.seatLockTtlSeconds());

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, userEmail, ttl);
        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Acquired seat lock on key {} for user {}", key, userEmail);
            return true;
        }

        // Check if already held by the exact same user, and refresh TTL if so
        String currentOwner = redisTemplate.opsForValue().get(key);
        if (currentOwner != null && currentOwner.equalsIgnoreCase(userEmail)) {
            redisTemplate.expire(key, ttl);
            log.debug("Refreshed seat lock TTL on key {} for existing owner {}", key, userEmail);
            return true;
        }

        log.debug("Failed to acquire seat lock on key {}: already locked by {}", key, currentOwner);
        return false;
    }

    public Optional<String> getLockOwner(
            String trainNumber,
            LocalDate journeyDate,
            String coachNumber,
            int seatNumber
    ) {
        String key = buildLockKey(trainNumber, journeyDate, coachNumber, seatNumber);
        String owner = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(owner);
    }

    public void releaseSeatLock(
            String trainNumber,
            LocalDate journeyDate,
            String coachNumber,
            int seatNumber,
            String userEmail
    ) {
        String key = buildLockKey(trainNumber, journeyDate, coachNumber, seatNumber);
        String currentOwner = redisTemplate.opsForValue().get(key);

        if (currentOwner != null && currentOwner.equalsIgnoreCase(userEmail)) {
            redisTemplate.delete(key);
            log.debug("Released seat lock on key {} by owner {}", key, userEmail);
        } else {
            log.warn("Cannot release seat lock on key {}: current owner is '{}', requester is '{}'",
                    key, currentOwner, userEmail);
        }
    }

    public String buildLockKey(String trainNumber, LocalDate journeyDate, String coachNumber, int seatNumber) {
        return LOCK_KEY_PREFIX + trainNumber + ":" + journeyDate + ":" + coachNumber + ":" + seatNumber;
    }
}
