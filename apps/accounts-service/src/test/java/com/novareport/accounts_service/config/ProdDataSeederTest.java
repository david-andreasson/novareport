package com.novareport.accounts_service.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ProdDataSeederTest {

    @Test
    void runDoesNotThrowWhenNoRepositoriesProvided() {
        assertThatCode(ProdDataSeeder::new).doesNotThrowAnyException();
    }
}
