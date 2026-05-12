# Member Withdrawal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 인증된 회원이 `DELETE /api/v1/auth/me`로 탈퇴하고, 기존 토큰과 소셜 계정 연결이 더 이상 사용할 수 없게 만든다.

**Architecture:** 회원 row는 과거 연습 세션의 FK 보존을 위해 삭제하지 않고 `withdrawn_at`으로 탈퇴 상태를 기록한다. 탈퇴 시 refresh token은 모두 폐기하고 social account 연결은 삭제해 같은 provider subject로 재가입할 수 있게 한다. 인증 필터는 access token의 회원이 존재하고 탈퇴하지 않았는지 DB에서 확인한다.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring Security, Spring Data JPA, Flyway, MockMvc integration tests.

---

### Task 1: 회원 탈퇴 API 테스트

**Files:**
- Modify: `src/test/java/com/saynow/auth/SocialAuthApiIntegrationTest.java`

- [ ] **Step 1: Write the failing withdrawal test**

```java
@Test
void withdrawRevokesTokensRejectsExistingAccessTokenAndAllowsFreshSocialSignup() throws Exception {
    JsonNode loginBody = login("withdraw-sub|withdraw@example.com|Withdraw User");
    String accessToken = loginBody.get("data").get("accessToken").asText();
    String refreshToken = loginBody.get("data").get("refreshToken").asText();
    String memberId = loginBody.get("data").get("member").get("memberId").asText();

    mockMvc.perform(delete("/api/v1/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(nullValue()));

    mockMvc.perform(post("/api/v1/auth/token/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"refreshToken":"%s"}
                            """.formatted(refreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));

    mockMvc.perform(post("/api/v1/sessions")
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"scenarioId":"cafe_iced_americano"}
                            """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));

    JsonNode reloginBody = login("withdraw-sub|withdraw@example.com|Withdraw User");
    assertThat(reloginBody.get("data").get("member").get("newMember").asBoolean()).isTrue();
    assertThat(reloginBody.get("data").get("member").get("memberId").asText()).isNotEqualTo(memberId);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest.withdrawRevokesTokensRejectsExistingAccessTokenAndAllowsFreshSocialSignup`

Expected: FAIL because `DELETE /api/v1/auth/me` is not mapped.

### Task 2: 탈퇴 상태와 저장소 동작 구현

**Files:**
- Create: `src/main/resources/db/migration/V4__member_withdrawal.sql`
- Modify: `src/main/java/com/saynow/auth/domain/Member.java`
- Modify: `src/main/java/com/saynow/auth/infrastructure/MemberRepository.java`
- Modify: `src/main/java/com/saynow/auth/infrastructure/RefreshTokenRepository.java`
- Modify: `src/main/java/com/saynow/auth/infrastructure/SocialAccountRepository.java`

- [ ] **Step 1: Add migration**

```sql
-- 회원 탈퇴 상태를 회원 테이블에 기록하는 마이그레이션
ALTER TABLE members ADD COLUMN withdrawn_at TIMESTAMP(6);
```

- [ ] **Step 2: Add domain state**

```java
@Column(name = "withdrawn_at")
private LocalDateTime withdrawnAt;

public void withdraw(LocalDateTime withdrawnAt) {
    this.nickname = null;
    this.email = null;
    this.withdrawnAt = withdrawnAt;
}

public boolean isActive() {
    return withdrawnAt == null;
}
```

- [ ] **Step 3: Add repository operations**

```java
Optional<Member> findByIdAndWithdrawnAtIsNull(Long id);
boolean existsByIdAndWithdrawnAtIsNull(Long id);

@Modifying
@Query("update RefreshToken token set token.revokedAt = :revokedAt where token.member.id = :memberId and token.revokedAt is null")
void revokeAllActiveByMemberId(Long memberId, LocalDateTime revokedAt);

@Modifying
@Query("delete from SocialAccount account where account.member.id = :memberId")
void deleteByMemberId(Long memberId);
```

### Task 3: 탈퇴 API와 인증 필터 구현

**Files:**
- Modify: `src/main/java/com/saynow/auth/api/AuthController.java`
- Modify: `src/main/java/com/saynow/auth/application/AuthService.java`
- Modify: `src/main/java/com/saynow/auth/security/AuthSecurityConfig.java`
- Modify: `src/main/java/com/saynow/auth/security/AuthTokenFilter.java`

- [ ] **Step 1: Add controller endpoint**

```java
@DeleteMapping("/me")
@Operation(summary = "회원 탈퇴", description = "현재 회원을 탈퇴 처리하고 refresh token과 소셜 계정 연결을 정리합니다.")
public ApiResponse<Void> withdraw(@AuthenticationPrincipal AuthMemberPrincipal principal) {
    authService.withdraw(principal.memberId());
    return ApiResponse.success(null);
}
```

- [ ] **Step 2: Add service logic**

```java
@Transactional
public void withdraw(Long memberId) {
    LocalDateTime now = LocalDateTime.now();
    Member member = memberRepository.findByIdAndWithdrawnAtIsNull(memberId)
            .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));
    refreshTokenRepository.revokeAllActiveByMemberId(memberId, now);
    socialAccountRepository.deleteByMemberId(memberId);
    member.withdraw(now);
}
```

- [ ] **Step 3: Protect the route and reject withdrawn members**

```java
.requestMatchers(HttpMethod.DELETE, "/api/v1/auth/me").authenticated()
```

```java
if (!memberRepository.existsByIdAndWithdrawnAtIsNull(memberId)) {
    failureResponseWriter.write(response, ErrorCode.AUTH_REQUIRED);
    return;
}
```

### Task 4: Green verification and commit

**Files:**
- Verify only.

- [ ] **Step 1: Run targeted auth test**

Run: `./gradlew test --tests com.saynow.auth.SocialAuthApiIntegrationTest`

Expected: PASS.

- [ ] **Step 2: Run security regression test**

Run: `./gradlew test --tests com.saynow.auth.SecurityAuthenticationIntegrationTest --tests com.saynow.practice.PracticeSessionAuthorizationIntegrationTest`

Expected: PASS.

- [ ] **Step 3: Run full verification**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/plans/2026-05-11-member-withdrawal.md checklist.md context-notes.md src/main src/test
git commit -m "feat: 회원 탈퇴 기능 추가"
```
