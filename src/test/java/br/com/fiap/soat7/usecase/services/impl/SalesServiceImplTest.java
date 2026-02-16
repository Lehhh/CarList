package br.com.fiap.soat7.usecase.services.impl;

import br.com.fiap.soat7.adapter.repositories.CarRepository;
import br.com.fiap.soat7.adapter.repositories.SaleRepository;
import br.com.fiap.soat7.data.domain.Car;
import br.com.fiap.soat7.data.domain.Sale;
import br.com.fiap.soat7.data.domain.dto.PaymentWebhookRequest;
import br.com.fiap.soat7.data.domain.dto.PurchaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesServiceImplTest {

    @Mock WebClient client;
    @Mock SaleRepository saleRepo;
    @Mock CarRepository carRepo;

    SalesServiceImpl service;

    @BeforeEach
    void setup() {
        service = new SalesServiceImpl(client, saleRepo, carRepo);
    }

    // ---- helper: stub do webclient só quando precisar (webhook PAID) ----
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubWebClientNotifyOk() {
        WebClient.RequestBodyUriSpec reqBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec reqBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec reqHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(client.post()).thenReturn(reqBodyUriSpec);
        when(reqBodyUriSpec.uri(Mockito.anyString(), Mockito.<Object>any()))
                .thenReturn(reqBodySpec);
        when(reqBodySpec.bodyValue(any())).thenReturn(reqHeadersSpec);
        when(reqHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());
    }

    // -----------------------------
    // listAvailable / listSold
    // -----------------------------

    @Test
    void listAvailable_deveDelegarRepo() {
        List<Car> expected = List.of(new Car());
        when(carRepo.findBySoldIsFalseOrderByPriceAsc()).thenReturn(expected);

        List<Car> actual = service.listAvailable();

        assertSame(expected, actual);
        verify(carRepo).findBySoldIsFalseOrderByPriceAsc();
        verifyNoMoreInteractions(carRepo);
        verifyNoInteractions(saleRepo, client);
    }

    @Test
    void listSold_deveDelegarRepo() {
        List<Car> expected = List.of(new Car());
        when(carRepo.findBySoldIsTrueOrderByPriceAsc()).thenReturn(expected);

        List<Car> actual = service.listSold();

        assertSame(expected, actual);
        verify(carRepo).findBySoldIsTrueOrderByPriceAsc();
        verifyNoMoreInteractions(carRepo);
        verifyNoInteractions(saleRepo, client);
    }

    // -----------------------------
    // purchase()
    // -----------------------------

    @Test
    void purchase_quandoCarNaoExiste_deveLancarIllegalArgument() {
        when(carRepo.findById(1L)).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class, () -> service.purchase(1L));
        assertEquals("Car não encontrado no serviço de venda", ex.getMessage());

        verify(carRepo).findById(1L);
        verifyNoInteractions(saleRepo, client);
    }

    @Test
    void purchase_quandoSaleNaoExiste_criaReservaESalva() {
        Car car = new Car();
        car.setPrice(new BigDecimal("12345.67"));
        when(carRepo.findById(10L)).thenReturn(Optional.of(car));
        when(saleRepo.lockByCarId(10L)).thenReturn(Optional.empty());

        when(saleRepo.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        PurchaseResponse resp = service.purchase(10L);

        assertEquals(99L, resp.saleId());
        assertEquals(10L, resp.carId());
        assertNotNull(resp.paymentCode());
        assertNotNull(resp.reservedUntil());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepo).save(captor.capture());

        Sale saved = captor.getValue();
        assertEquals(10L, saved.getCarId());
        assertEquals(Sale.Status.RESERVED, saved.getStatus());
        assertEquals(new BigDecimal("12345.67"), saved.getLockedPrice());
        assertNotNull(saved.getPaymentCode());
        assertNotNull(saved.getReservedUntil());

        verify(saleRepo).lockByCarId(10L);
        verify(carRepo).findById(10L);
        verifyNoInteractions(client);
    }

    @Test
    void purchase_quandoSaleJaPago_lancaIllegalState() {
        Car car = new Car();
        car.setPrice(new BigDecimal("10"));
        when(carRepo.findById(1L)).thenReturn(Optional.of(car));

        Sale sale = new Sale();
        sale.setCarId(1L);
        sale.setStatus(Sale.Status.PAID);
        when(saleRepo.lockByCarId(1L)).thenReturn(Optional.of(sale));

        var ex = assertThrows(IllegalStateException.class, () -> service.purchase(1L));
        assertEquals("Car já foi vendido", ex.getMessage());

        verify(saleRepo, never()).save(any());
        verifyNoInteractions(client);
    }

    @Test
    void purchase_quandoSaleReservadoEValido_lancaIllegalState() {
        Car car = new Car();
        car.setPrice(new BigDecimal("10"));
        when(carRepo.findById(2L)).thenReturn(Optional.of(car));

        Sale sale = new Sale();
        sale.setCarId(2L);
        sale.setStatus(Sale.Status.RESERVED);
        sale.setReservedUntil(Instant.now().plusSeconds(60));
        when(saleRepo.lockByCarId(2L)).thenReturn(Optional.of(sale));

        var ex = assertThrows(IllegalStateException.class, () -> service.purchase(2L));
        assertEquals("Car já está reservado", ex.getMessage());

        verify(saleRepo, never()).save(any());
        verifyNoInteractions(client);
    }

    @Test
    void purchase_quandoSaleReservadoMasExpirado_reservaDeNovo() {
        Car car = new Car();
        car.setPrice(new BigDecimal("10"));
        when(carRepo.findById(3L)).thenReturn(Optional.of(car));

        Sale sale = new Sale();
        sale.setId(7L);
        sale.setCarId(3L);
        sale.setLockedPrice(new BigDecimal("10"));
        sale.setStatus(Sale.Status.RESERVED);
        sale.setReservedUntil(Instant.now().minusSeconds(5));
        when(saleRepo.lockByCarId(3L)).thenReturn(Optional.of(sale));

        when(saleRepo.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseResponse resp = service.purchase(3L);

        assertEquals(7L, resp.saleId());
        assertEquals(3L, resp.carId());
        assertNotNull(resp.paymentCode());
        assertNotNull(resp.reservedUntil());

        verify(saleRepo).save(sale);
        assertEquals(Sale.Status.RESERVED, sale.getStatus());
        assertNotNull(sale.getPaymentCode());
        assertTrue(sale.getReservedUntil().isAfter(Instant.now()));

        verifyNoInteractions(client);
    }

    // -----------------------------
    // handlePaymentWebhook()
    // -----------------------------

    @Test
    void webhook_quandoPaymentCodeNaoExiste_lancaIllegalArgument() {
        when(saleRepo.findByPaymentCode("p")).thenReturn(Optional.empty());
        PaymentWebhookRequest req = new PaymentWebhookRequest("p", "PAID", "123", Instant.now());

        var ex = assertThrows(IllegalArgumentException.class, () -> service.handlePaymentWebhook(req));
        assertEquals("paymentCode não encontrado", ex.getMessage());

        verifyNoInteractions(carRepo, client);
    }

    @Test
    void webhook_quandoCarNaoExiste_lancaIllegalArgument() {
        Sale sale = new Sale();
        sale.setCarId(10L);
        when(saleRepo.findByPaymentCode("p")).thenReturn(Optional.of(sale));
        when(carRepo.findById(10L)).thenReturn(Optional.empty());

        PaymentWebhookRequest req = new PaymentWebhookRequest("p", "PAID", "123", Instant.now());

        var ex = assertThrows(IllegalArgumentException.class, () -> service.handlePaymentWebhook(req));
        assertEquals("Car não encontrado no serviço de venda", ex.getMessage());

        verifyNoInteractions(client);
    }

    @Test
    void webhook_idempotencia_jaPagoEStatusPaid_retornaSemSalvar() {
        Sale sale = new Sale();
        sale.setCarId(1L);
        sale.setStatus(Sale.Status.PAID);
        when(saleRepo.findByPaymentCode("pc")).thenReturn(Optional.of(sale));
        when(carRepo.findById(1L)).thenReturn(Optional.of(new Car()));

        PaymentWebhookRequest req = new PaymentWebhookRequest("pc", "PAID", "cpf", Instant.now());

        service.handlePaymentWebhook(req);

        verify(saleRepo, never()).save(any());
        verify(carRepo, never()).save(any());
        verifyNoInteractions(client);
    }

    @Test
    void webhook_idempotencia_jaCanceladoEStatusCanceled_retornaSemSalvar() {
        Sale sale = new Sale();
        sale.setCarId(2L);
        sale.setStatus(Sale.Status.CANCELED);
        when(saleRepo.findByPaymentCode("pc2")).thenReturn(Optional.of(sale));
        when(carRepo.findById(2L)).thenReturn(Optional.of(new Car()));

        PaymentWebhookRequest req = new PaymentWebhookRequest("pc2", "CANCELED", "cpf", Instant.now());

        service.handlePaymentWebhook(req);

        verify(saleRepo, never()).save(any());
        verify(carRepo, never()).save(any());
        verifyNoInteractions(client);
    }

    @Test
    void webhook_statusPaid_atualizaSale_salva_notificaCore_eventAtNull() {
        stubWebClientNotifyOk(); // <- só aqui

        Sale sale = new Sale();
        sale.setCarId(3L);
        sale.setPaymentCode("pc3");
        sale.setStatus(Sale.Status.RESERVED);
        sale.setReservedUntil(Instant.now().plusSeconds(60));
        when(saleRepo.findByPaymentCode("pc3")).thenReturn(Optional.of(sale));
        when(carRepo.findById(3L)).thenReturn(Optional.of(new Car()));

        PaymentWebhookRequest req = new PaymentWebhookRequest("pc3", "paid", "11122233344", null);

        service.handlePaymentWebhook(req);

        assertEquals(Sale.Status.PAID, sale.getStatus());
        assertEquals("11122233344", sale.getBuyerCpf());
        assertNotNull(sale.getSoldAt());
        assertNull(sale.getReservedUntil());

        verify(saleRepo).save(sale);
        verify(client).post(); // notificação executada (subscribe)
    }

    @Test
    void webhook_statusPaid_atualizaSale_salva_notificaCore_eventAtInformado() {
        stubWebClientNotifyOk(); // <- só aqui

        Sale sale = new Sale();
        sale.setCarId(4L);
        sale.setPaymentCode("pc4");
        sale.setStatus(Sale.Status.RESERVED);
        when(saleRepo.findByPaymentCode("pc4")).thenReturn(Optional.of(sale));
        when(carRepo.findById(4L)).thenReturn(Optional.of(new Car()));

        Instant eventAt = Instant.now().minusSeconds(10);
        PaymentWebhookRequest req = new PaymentWebhookRequest("pc4", "PAID", "cpf", eventAt);

        service.handlePaymentWebhook(req);

        assertEquals(Sale.Status.PAID, sale.getStatus());
        assertEquals(eventAt, sale.getSoldAt());

        verify(saleRepo).save(sale);
        verify(client).post();
    }

    @Test
    void webhook_statusCanceled_atualizaSaleCar_salva_semNotificarCore() {
        Sale sale = new Sale();
        sale.setCarId(5L);
        sale.setPaymentCode("pc5");
        sale.setStatus(Sale.Status.RESERVED);
        sale.setReservedUntil(Instant.now().plusSeconds(60));
        when(saleRepo.findByPaymentCode("pc5")).thenReturn(Optional.of(sale));

        Car car = new Car();
        car.setSold(true);
        when(carRepo.findById(5L)).thenReturn(Optional.of(car));

        PaymentWebhookRequest req = new PaymentWebhookRequest("pc5", "CANCELED", "cpf", Instant.now());

        service.handlePaymentWebhook(req);

        assertEquals(Sale.Status.CANCELED, sale.getStatus());
        assertNull(sale.getReservedUntil());
        assertFalse(car.isSold());

        verify(carRepo).save(car);
        verify(saleRepo).save(sale);
        verifyNoInteractions(client);
    }

    @Test
    void webhook_statusInvalido_lancaIllegalArgument() {
        Sale sale = new Sale();
        sale.setCarId(6L);
        sale.setPaymentCode("pc6");
        sale.setStatus(Sale.Status.RESERVED);
        when(saleRepo.findByPaymentCode("pc6")).thenReturn(Optional.of(sale));
        when(carRepo.findById(6L)).thenReturn(Optional.of(new Car()));

        PaymentWebhookRequest req = new PaymentWebhookRequest("pc6", "WHATEVER", "cpf", Instant.now());

        var ex = assertThrows(IllegalArgumentException.class, () -> service.handlePaymentWebhook(req));
        assertEquals("status inválido (use PAID ou CANCELED)", ex.getMessage());

        verify(saleRepo, never()).save(any());
        verify(carRepo, never()).save(any());
        verifyNoInteractions(client);
    }
}
