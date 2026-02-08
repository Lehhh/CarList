package br.com.fiap.soat7.usecase.services.impl;

import br.com.fiap.soat7.adapter.repositories.CarRepository;
import br.com.fiap.soat7.adapter.repositories.SaleRepository;
import br.com.fiap.soat7.data.domain.Car;
import br.com.fiap.soat7.data.domain.Sale;
import br.com.fiap.soat7.data.domain.dto.PurchaseResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SalesServiceImplTest {

    @Mock WebClient client;
    @Mock SaleRepository saleRepo;
    @Mock CarRepository carRepo;

    @InjectMocks
    SalesServiceImpl service;

    @Captor ArgumentCaptor<Sale> saleCaptor;

    @Test
    void purchase_shouldThrow400_whenCarNotFound() {
        when(carRepo.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.purchase(1L));

        assertEquals("Car não encontrado no serviço de venda", ex.getMessage());
        verifyNoInteractions(saleRepo);
    }

    @Test
    void purchase_shouldThrow409_whenAlreadyPaid() {
        Car car = new Car();
        car.setId(1L);
        car.setPrice(new BigDecimal("1000.00"));

        Sale sale = new Sale();
        sale.setCarId(1L);
        sale.setStatus(Sale.Status.PAID);

        when(carRepo.findById(1L)).thenReturn(Optional.of(car));
        when(saleRepo.lockByCarId(1L)).thenReturn(Optional.of(sale));

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> service.purchase(1L));

        assertEquals("Car já foi vendido", ex.getMessage());
        verify(saleRepo, never()).save(any());
    }

    @Test
    void purchase_shouldThrow409_whenReservedAndStillValid() {
        Car car = new Car();
        car.setId(1L);
        car.setPrice(new BigDecimal("1000.00"));

        Sale sale = new Sale();
        sale.setCarId(1L);
        sale.setStatus(Sale.Status.RESERVED);
        sale.setReservedUntil(Instant.now().plusSeconds(60)); // ainda válido

        when(carRepo.findById(1L)).thenReturn(Optional.of(car));
        when(saleRepo.lockByCarId(1L)).thenReturn(Optional.of(sale));

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> service.purchase(1L));

        assertEquals("Car já está reservado", ex.getMessage());
        verify(saleRepo, never()).save(any());
    }

    @Test
    void purchase_shouldReserveAndReturnPurchaseResponse_whenAvailable() {
        Car car = new Car();
        car.setId(1L);
        car.setPrice(new BigDecimal("99999.99"));

        // lock retorna vazio -> cria nova Sale
        when(carRepo.findById(1L)).thenReturn(Optional.of(car));
        when(saleRepo.lockByCarId(1L)).thenReturn(Optional.empty());

        // simula save atribuindo id
        when(saleRepo.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(123L);
            return s;
        });

        PurchaseResponse res = service.purchase(1L);

        assertNotNull(res);
        assertEquals(123L, res.saleId());
        assertEquals(1L, res.carId());
        assertNotNull(res.paymentCode());
        assertNotNull(res.reservedUntil());

        verify(saleRepo).save(saleCaptor.capture());
        Sale saved = saleCaptor.getValue();

        assertEquals(1L, saved.getCarId());
        assertEquals(new BigDecimal("99999.99"), saved.getLockedPrice());
        assertEquals(Sale.Status.RESERVED, saved.getStatus());
        assertNotNull(saved.getPaymentCode());
        assertNotNull(saved.getReservedUntil());
        assertTrue(saved.getReservedUntil().isAfter(Instant.now()));
    }

    @Test
    void purchase_shouldCreateSaleWhenLockReturnsEmpty_andSaveReserved() {
        // given
        Long carId = 77L;

        Car car = new Car();
        car.setId(carId);
        car.setPrice(new BigDecimal("55555.55"));

        when(carRepo.findById(carId)).thenReturn(Optional.of(car));
        when(saleRepo.lockByCarId(carId)).thenReturn(Optional.empty()); // <<< cobre sale == null

        // save: simula id gerado
        when(saleRepo.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(999L);
            return s;
        });

        // when
        PurchaseResponse res = service.purchase(carId);

        // then
        assertNotNull(res);
        assertEquals(999L, res.saleId());
        assertEquals(carId, res.carId());
        assertNotNull(res.paymentCode());
        assertNotNull(res.reservedUntil());

        verify(saleRepo).save(saleCaptor.capture());
        Sale saved = saleCaptor.getValue();

        // valida que veio do car (linhas 31–35)
        assertEquals(carId, saved.getCarId());
        assertEquals(new BigDecimal("55555.55"), saved.getLockedPrice());

        // após o bloco 29–36, ele continua e vira RESERVED
        assertEquals(Sale.Status.RESERVED, saved.getStatus());
        assertNotNull(saved.getPaymentCode());
        assertNotNull(saved.getReservedUntil());
    }

    @Test
    void listAvailable_shouldQueryRepoWithAvailable() {
        SalesServiceImpl service = new SalesServiceImpl(client, saleRepo, carRepo);

        List<Sale> expected = List.of(new Sale(), new Sale());
        when(saleRepo.findByStatusOrderByLockedPriceAsc(Sale.Status.AVAILABLE)).thenReturn(expected);

        List<Sale> result = service.listAvailable();

        assertSame(expected, result);
        verify(saleRepo).findByStatusOrderByLockedPriceAsc(Sale.Status.AVAILABLE);
        verifyNoMoreInteractions(saleRepo);
        verifyNoInteractions(client, carRepo);
    }

    @Test
    void listSold_shouldQueryRepoWithPaid() {
        SalesServiceImpl service = new SalesServiceImpl(client, saleRepo, carRepo);

        List<Sale> expected = List.of(new Sale());
        when(saleRepo.findByStatusOrderByLockedPriceAsc(Sale.Status.PAID)).thenReturn(expected);

        List<Sale> result = service.listSold();

        assertSame(expected, result);
        verify(saleRepo).findByStatusOrderByLockedPriceAsc(Sale.Status.PAID);
        verifyNoMoreInteractions(saleRepo);
        verifyNoInteractions(client, carRepo);
    }


}
