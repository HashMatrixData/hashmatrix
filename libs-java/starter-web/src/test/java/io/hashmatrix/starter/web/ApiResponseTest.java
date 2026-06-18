package io.hashmatrix.starter.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void okWithDataIsSuccess() {
        ApiResponse<String> response = ApiResponse.ok("payload");
        assertThat(response.code()).isEqualTo("0");
        assertThat(response.message()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void okWithoutDataIsSuccess() {
        ApiResponse<Void> response = ApiResponse.ok();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.data()).isNull();
    }

    @Test
    void failCarriesCodeAndMessageAndIsNotSuccess() {
        ApiResponse<Void> response = ApiResponse.fail("E001", "boom");
        assertThat(response.code()).isEqualTo("E001");
        assertThat(response.message()).isEqualTo("boom");
        assertThat(response.data()).isNull();
        assertThat(response.isSuccess()).isFalse();
    }
}
