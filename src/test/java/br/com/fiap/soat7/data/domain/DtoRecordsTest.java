package br.com.fiap.soat7.data.domain;

import br.com.fiap.soat7.data.domain.dto.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoRecordsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (factory != null) factory.close();
    }

    @Test
    void carSoldEvent_deveCriarEExporComponentes() {
        Instant soldAt = Instant.parse("2026-02-08T10:00:00Z");

        CarSoldEvent dto = new CarSoldEvent(55L, "12345678901", soldAt, "PAY-123");

        assertThat(dto.carId()).isEqualTo(55L);
        assertThat(dto.buyerCpf()).isEqualTo("12345678901");
        assertThat(dto.soldAt()).isEqualTo(soldAt);
        assertThat(dto.paymentCode()).isEqualTo("PAY-123");
    }

    @Test
    void carSyncRequest_deveCriarEExporComponentes() {
        Instant updatedAt = Instant.parse("2026-02-08T10:00:00Z");

        CarSyncRequest dto = new CarSyncRequest(
                1L,
                "VW",
                "Golf",
                2017,
                "Cinza",
                new BigDecimal("70000.00"),
                updatedAt
        );

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.brand()).isEqualTo("VW");
        assertThat(dto.model()).isEqualTo("Golf");
        assertThat(dto.year()).isEqualTo(2017);
        assertThat(dto.color()).isEqualTo("Cinza");
        assertThat(dto.price()).isEqualByComparingTo("70000.00");
        assertThat(dto.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void paymentWebhookRequest_deveCriarEExporComponentes() {
        Instant eventAt = Instant.parse("2026-02-08T10:00:00Z");

        PaymentWebhookRequest dto = new PaymentWebhookRequest(
                "PAY-123",
                "PAID",
                "12345678901",
                eventAt
        );

        assertThat(dto.paymentCode()).isEqualTo("PAY-123");
        assertThat(dto.status()).isEqualTo("PAID");
        assertThat(dto.buyerCpf()).isEqualTo("12345678901");
        assertThat(dto.eventAt()).isEqualTo(eventAt);
    }

    @Test
    void carSellRequest_quandoValido_naoDeveTerViolacoes() {
        CarSellRequest dto = new CarSellRequest("12345678901", LocalDateTime.of(2026, 2, 8, 10, 0));

        Set<ConstraintViolation<CarSellRequest>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void carSellRequest_quandoBuyerCpfEmBranco_deveFalharNotBlank() {
        CarSellRequest dto = new CarSellRequest("   ", LocalDateTime.of(2026, 2, 8, 10, 0));

        Set<ConstraintViolation<CarSellRequest>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("buyerCpf");
    }

    @Test
    void carSellRequest_quandoBuyerCpfNaoTem11Digitos_deveFalharPattern() {
        CarSellRequest dto = new CarSellRequest("123", LocalDateTime.of(2026, 2, 8, 10, 0));

        Set<ConstraintViolation<CarSellRequest>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("buyerCpf");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .anyMatch(msg -> msg.contains("CPF deve ter 11 dígitos"));
    }

    @Test
    void carSellRequest_quandoSoldAtNulo_deveFalharNotNull() {
        CarSellRequest dto = new CarSellRequest("12345678901", null);

        Set<ConstraintViolation<CarSellRequest>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("soldAt");
    }

    @Test
    void purchaseRequest_deveCriarEExporComponentes() {
        PurchaseRequest dto = new PurchaseRequest(99L);

        assertThat(dto.carId()).isEqualTo(99L);
    }

    @Test
    void purchaseResponse_deveCriarEExporComponentes() {
        Instant reservedUntil = Instant.parse("2026-02-08T11:00:00Z");

        PurchaseResponse dto = new PurchaseResponse(10L, 55L, "PAY-123", reservedUntil);

        assertThat(dto.saleId()).isEqualTo(10L);
        assertThat(dto.carId()).isEqualTo(55L);
        assertThat(dto.paymentCode()).isEqualTo("PAY-123");
        assertThat(dto.reservedUntil()).isEqualTo(reservedUntil);
    }
}
