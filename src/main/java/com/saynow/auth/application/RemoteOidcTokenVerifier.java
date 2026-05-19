// 실제 OIDC ID Token의 서명과 claim을 검증하는 verifier
package com.saynow.auth.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.auth.domain.SocialProvider;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "saynow.auth.oidc.fake-enabled", havingValue = "false", matchIfMissing = true)
public class RemoteOidcTokenVerifier implements OidcTokenVerifier {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 300;

    private final OidcProperties oidcProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<SocialProvider, JsonNode> jwksCache = new ConcurrentHashMap<>();

    @Override
    public OidcUserInfo verify(SocialProvider provider, String idToken, String nonce) {
        try {
            ProviderSettings settings = settings(provider);
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
            }

            Map<String, Object> header = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[0]), MAP_TYPE);
            Map<String, Object> claims = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), MAP_TYPE);
            if (!"RS256".equals(header.get("alg"))) {
                throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
            }

            PublicKey publicKey = publicKey(provider, settings, header.get("kid"));
            verifySignature(parts, publicKey);
            verifyClaims(settings, claims, nonce);

            String subject = text(claims.get("sub"));
            if (subject == null || subject.isBlank()) {
                throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
            }
            String nickname = text(claims.get("name"));
            if (nickname == null || nickname.isBlank()) {
                nickname = text(claims.get("nickname"));
            }
            return new OidcUserInfo(subject, text(claims.get("email")), booleanValue(claims.get("email_verified")), nickname);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }
    }

    private ProviderSettings settings(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> new ProviderSettings(
                    List.of("https://accounts.google.com", "accounts.google.com"),
                    "https://www.googleapis.com/oauth2/v3/certs",
                    configuredAudiences(oidcProperties.getGoogleAudiences()),
                    oidcProperties.isNonceRequired()
            );
            case KAKAO -> new ProviderSettings(
                    List.of("https://kauth.kakao.com"),
                    "https://kauth.kakao.com/.well-known/jwks.json",
                    configuredAudiences(oidcProperties.getKakaoAudiences()),
                    oidcProperties.isNonceRequired()
            );
        };
    }

    private List<String> configuredAudiences(List<String> audiences) {
        return audiences.stream()
                .filter(audience -> audience != null && !audience.isBlank())
                .toList();
    }

    private PublicKey publicKey(SocialProvider provider, ProviderSettings settings, Object kid) throws Exception {
        if (kid == null) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }
        JsonNode jwks = jwksCache.computeIfAbsent(provider, ignored -> fetchJwks(settings.jwksUri()));
        JsonNode key = findKey(jwks, kid.toString());
        if (key == null) {
            jwks = fetchJwks(settings.jwksUri());
            jwksCache.put(provider, jwks);
            key = findKey(jwks, kid.toString());
        }
        if (key == null) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }

        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("n").asText()));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("e").asText()));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private JsonNode fetchJwks(String jwksUri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(jwksUri)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(ErrorCode.OIDC_PROVIDER_UNAVAILABLE);
            }
            return objectMapper.readTree(response.body());
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.OIDC_PROVIDER_UNAVAILABLE);
        }
    }

    private JsonNode findKey(JsonNode jwks, String kid) {
        for (JsonNode key : jwks.path("keys")) {
            if (kid.equals(key.path("kid").asText())) {
                return key;
            }
        }
        return null;
    }

    private void verifySignature(String[] parts, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update((parts[0] + "." + parts[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!signature.verify(Base64.getUrlDecoder().decode(parts[2]))) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }
    }

    private void verifyClaims(ProviderSettings settings, Map<String, Object> claims, String nonce) {
        if (!settings.issuers().contains(text(claims.get("iss")))) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }
        if (settings.audiences().isEmpty() || !containsAudience(claims.get("aud"), settings.audiences())) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }
        long now = Instant.now().getEpochSecond();
        if (longValue(claims.get("exp")) <= now - ALLOWED_CLOCK_SKEW_SECONDS) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }
        Long issuedAt = longValue(claims.get("iat"));
        if (issuedAt != null && issuedAt > now + ALLOWED_CLOCK_SKEW_SECONDS) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }
        if (settings.nonceRequired()
                && (nonce == null || nonce.isBlank()
                || !MessageDigest.isEqual(nonce.getBytes(), text(claims.get("nonce")).getBytes()))) {
            throw new ApiException(ErrorCode.OIDC_NONCE_MISMATCH);
        }
    }

    private boolean containsAudience(Object claim, List<String> audiences) {
        if (claim instanceof String audience) {
            return audiences.contains(audience);
        }
        if (claim instanceof List<?> values) {
            return values.stream().map(String::valueOf).anyMatch(audiences::contains);
        }
        return false;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private Boolean booleanValue(Object value) {
        return value instanceof Boolean bool ? bool : null;
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private record ProviderSettings(List<String> issuers, String jwksUri, List<String> audiences, boolean nonceRequired) {
    }
}
