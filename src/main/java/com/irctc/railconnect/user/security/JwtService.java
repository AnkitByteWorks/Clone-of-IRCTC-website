package com.irctc.railconnect.user.security;

import com.irctc.railconnect.user.domain.UserRole;
import com.irctc.railconnect.user.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HEADER_BASE64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));

    private final String secretKey;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${app.jwt.secret:RailConnectDefaultSuperSecretSigningKeyForDevelopment2026!}") String secretKey,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds,
            ObjectMapper objectMapper
    ) {
        this.secretKey = secretKey;
        this.expirationSeconds = expirationSeconds;
        this.objectMapper = objectMapper;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String generateToken(UserEntity user) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expirationSeconds;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getEmail());
        payload.put("userId", user.getId());
        payload.put("role", user.getRole().name());
        payload.put("iat", now);
        payload.put("exp", exp);

        try {
            byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);
            String signingInput = HEADER_BASE64 + "." + payloadBase64;
            String signature = sign(signingInput);

            return signingInput + "." + signature;
        } catch (Exception ex) {
            log.error("Failed to generate JWT token for user: {}", user.getEmail(), ex);
            throw new RuntimeException("Error generating authentication token", ex);
        }
    }

    public Optional<AuthenticatedUser> validateAndExtractUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);

        // Constant-time signature comparison against timing attacks
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = parts[2].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            log.debug("JWT token signature mismatch");
            return Optional.empty();
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = objectMapper.readTree(payloadBytes);

            long exp = node.path("exp").asLong(0);
            if (exp <= Instant.now().getEpochSecond()) {
                log.debug("JWT token has expired");
                return Optional.empty();
            }

            Long userId = node.path("userId").asLong();
            String email = node.path("sub").asText();
            String roleStr = node.path("role").asText();

            if (userId == 0 || email.isBlank() || roleStr.isBlank()) {
                return Optional.empty();
            }

            UserRole role = UserRole.valueOf(roleStr);
            return Optional.of(new AuthenticatedUser(userId, email, role));
        } catch (Exception ex) {
            log.debug("Failed to parse and validate JWT payload: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String sign(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signatureBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to sign JWT with HMAC-SHA256", ex);
        }
    }
}
