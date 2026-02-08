package br.com.fiap.soat7.infra.config.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerWithSalesControllerTest {


    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void badRequest_shouldReturn400_andErrorMessage() {
        var ex = new IllegalArgumentException("carId inválido");

        ResponseEntity<Map<String, Object>> res = handler.badRequest(ex);

        assertEquals(400, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals("carId inválido", res.getBody().get("error"));
    }

    @Test
    void conflict_shouldReturn409_andErrorMessage() {
        var ex = new IllegalStateException("pagamento já processado");

        ResponseEntity<Map<String, Object>> res = handler.conflict(ex);

        assertEquals(409, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals("pagamento já processado", res.getBody().get("error"));
    }
}
