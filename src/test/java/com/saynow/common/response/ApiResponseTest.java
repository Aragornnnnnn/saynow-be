// 공통 API 응답 객체의 생성 헬퍼를 검증한다.
package com.saynow.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successWithStatusReturnsResponseEntityUsingGivenHttpStatus() {
        ResponseEntity<ApiResponse<String>> response = ApiResponse.success(HttpStatus.CREATED, "created");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isEqualTo("created");
        assertThat(response.getBody().error()).isNull();
    }
}
