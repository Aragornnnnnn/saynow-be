// SayNow 자체 access token과 refresh token을 발급하고 검증하는 서비스
package com.saynow.auth.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SaynowTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final TokenProperties tokenProperties;
    private final ObjectMapper objectMapper;

    public String createAccessToken(Long memberId) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("typ", ACCESS_TOKEN_TYPE);
            payload.put("sub", memberId.toString());
            payload.put("exp", Instant.now().plusSeconds(tokenProperties.getAccessExpiresInSeconds()).getEpochSecond());

            String headerPart = base64Url(objectMapper.writeValueAsBytes(header));
            String payloadPart = base64Url(objectMapper.writeValueAsBytes(payload));
            String signingInput = headerPart + "." + payloadPart;
            return signingInput + "." + sign(signingInput);
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public Long parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new ApiException(ErrorCode.AUTH_REQUIRED);
            }

            String signingInput = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(signingInput).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new ApiException(ErrorCode.AUTH_REQUIRED);
            }

            Map<String, Object> claims = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), CLAIMS_TYPE);
            if (!ACCESS_TOKEN_TYPE.equals(claims.get("typ"))) {
                throw new ApiException(ErrorCode.AUTH_REQUIRED);
            }
            long expiresAt = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new ApiException(ErrorCode.ACCESS_TOKEN_EXPIRED);
            }
            return Long.valueOf(claims.get("sub").toString());
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED);
        }
    }

    public String createRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return base64Url(bytes);
    }

    public String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return base64Url(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(tokenProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
