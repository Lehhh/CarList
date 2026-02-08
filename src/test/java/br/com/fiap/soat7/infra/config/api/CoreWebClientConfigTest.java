package br.com.fiap.soat7.infra.config.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class CoreWebClientConfigTest {

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateWebClient() {
        CoreWebClientConfig config = new CoreWebClientConfig();

        var client = config.coreWebClient("https://api.test.com");

        assertNotNull(client);
    }

}