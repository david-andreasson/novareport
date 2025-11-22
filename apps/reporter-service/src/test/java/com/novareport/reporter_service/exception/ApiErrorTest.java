package com.novareport.reporter_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {

    @Test
    void recordHoldsValues() {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, "ERR", "message");

        assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error.error()).isEqualTo("ERR");
        assertThat(error.message()).isEqualTo("message");
    }
}
