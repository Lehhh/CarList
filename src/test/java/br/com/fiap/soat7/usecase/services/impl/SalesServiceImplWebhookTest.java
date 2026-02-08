package br.com.fiap.soat7.usecase.services.impl;

import br.com.fiap.soat7.adapter.repositories.CarRepository;
import br.com.fiap.soat7.adapter.repositories.SaleRepository;
import br.com.fiap.soat7.data.domain.Sale;
import br.com.fiap.soat7.data.domain.dto.PaymentWebhookRequest;
import br.com.fiap.soat7.usecase.services.impl.SalesServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SalesServiceImplWebhookTest {

    @Mock WebClient client;
    @Mock SaleRepository saleRepo;
    @Mock CarRepository carRepo; // não usado aqui, mas faz parte do ctor

    @InjectMocks
    SalesServiceImpl service;

    // mocks da chain do WebClient
    @Mock WebClient.RequestBodyUriSpec reqBodyUriSpec;
    @Mock WebClient.RequestBodySpec reqBodySpec;
    @Mock WebClient.RequestHeadersSpec<?> reqHeadersSpec;
    @Mock WebClient.ResponseSpec responseSpec;

    @Captor ArgumentCaptor<Sale> saleCaptor;

    @Test
    void handlePaymentWebhook_shouldThrow400_whenPaymentCodeNotFound() {
        when(saleRepo.findByPaymentCode("X")).thenReturn(Optional.empty());

        var req = new PaymentWebhookRequest("X", "PAID", "123", Instant.now());

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.handlePaymentWebhook(req));

        assertEquals("paymentCode não encontrado", ex.getMessage());
        verifyNoMoreInteractions(client);
    }

    @Test
    void handlePaymentWebhook_shouldBeIdempotent_whenAlreadyPaidAndStatusPaid() {
        Sale sale = new Sale();
        sale.setStatus(Sale.Status.PAID);

        when(saleRepo.findByPaymentCode("P1")).thenReturn(Optional.of(sale));

        var req = new PaymentWebhookRequest("P1", "PAID", "123", Instant.now());

        service.handlePaymentWebhook(req);

        verify(saleRepo, never()).save(any());
        verifyNoInteractions(client);
    }

    @Test
    void handlePaymentWebhook_shouldMarkPaid_save_andNotifyCore() {
        Sale sale = new Sale();
        sale.setId(9L);
        sale.setCarId(77L);
        sale.setPaymentCode("P2");
        sale.setStatus(Sale.Status.RESERVED);
        sale.setReservedUntil(Instant.now().plusSeconds(600));

        when(saleRepo.findByPaymentCode("P2")).thenReturn(Optional.of(sale));
        when(saleRepo.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        // mock chain do WebClient
        when(client.post()).thenReturn(reqBodyUriSpec);
        when(reqBodyUriSpec.uri(eq("/internal/cars/{carId}/sold"), eq(77L))).thenReturn(reqBodySpec);

        // >>> AQUI: bodyValue retorna RequestHeadersSpec<?> (genéricos), então fazemos cast raw pra evitar capture mismatch
        @SuppressWarnings({"rawtypes", "unchecked"})
        WebClient.RequestHeadersSpec headersSpec = (WebClient.RequestHeadersSpec) reqBodySpec;
        when(reqBodySpec.bodyValue(any())).thenReturn(headersSpec);

        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.noContent().build()));

        var eventAt = Instant.parse("2026-02-08T12:00:00Z");
        var req = new PaymentWebhookRequest("P2", "PAID", "12345678900", eventAt);

        service.handlePaymentWebhook(req);

        verify(saleRepo).save(saleCaptor.capture());
        Sale saved = saleCaptor.getValue();

        assertEquals(Sale.Status.PAID, saved.getStatus());
        assertEquals("12345678900", saved.getBuyerCpf());
        assertEquals(eventAt, saved.getSoldAt());
        assertNull(saved.getReservedUntil());

        // garantiu que chamou o core
        verify(client).post();
        verify(reqBodyUriSpec).uri("/internal/cars/{carId}/sold", 77L);
        verify(reqBodySpec).bodyValue(any());
        verify(responseSpec).toBodilessEntity();
    }


    @Test
    void handlePaymentWebhook_shouldMarkCanceled_andSave() {
        Sale sale = new Sale();
        sale.setStatus(Sale.Status.RESERVED);
        sale.setReservedUntil(Instant.now().plusSeconds(600));

        when(saleRepo.findByPaymentCode("P3")).thenReturn(Optional.of(sale));
        when(saleRepo.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new PaymentWebhookRequest("P3", "CANCELED", "123", Instant.now());

        service.handlePaymentWebhook(req);

        verify(saleRepo).save(saleCaptor.capture());
        Sale saved = saleCaptor.getValue();

        assertEquals(Sale.Status.CANCELED, saved.getStatus());
        assertNull(saved.getReservedUntil());

        verifyNoInteractions(client);
    }

    @Test
    void handlePaymentWebhook_shouldThrow400_whenStatusInvalid() {
        Sale sale = new Sale();
        sale.setStatus(Sale.Status.RESERVED);

        when(saleRepo.findByPaymentCode("P4")).thenReturn(Optional.of(sale));

        var req = new PaymentWebhookRequest("P4", "UNKNOWN", "123", Instant.now());

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.handlePaymentWebhook(req));

        assertEquals("status inválido (use PAID ou CANCELED)", ex.getMessage());

        verify(saleRepo, never()).save(any());
        verifyNoInteractions(client);
    }
}
