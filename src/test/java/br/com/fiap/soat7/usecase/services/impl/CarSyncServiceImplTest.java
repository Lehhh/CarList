package br.com.fiap.soat7.usecase.services.impl;

import br.com.fiap.soat7.adapter.repositories.CarRepository;
import br.com.fiap.soat7.data.domain.Car;
import br.com.fiap.soat7.data.domain.dto.CarSyncRequest;
import br.com.fiap.soat7.usecase.services.impl.CarSyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CarSyncServiceImplTest {

    @Mock
    CarRepository carRepo;

    @InjectMocks
    CarSyncServiceImpl service;

    @Captor
    ArgumentCaptor<Car> carCaptor;

    @Test
    void upsert_shouldCreateNewCar_whenNotFound() {
        CarSyncRequest req = new CarSyncRequest(
                10L,
                "Toyota",
                "Corolla",
                2020,
                "Prata",
                new BigDecimal("95000.00"),
                Instant.now() // ou Instant.parse("2026-01-01T00:00:00Z")
        );

        when(carRepo.findById(10L)).thenReturn(Optional.empty());
        when(carRepo.save(any(Car.class))).thenAnswer(inv -> inv.getArgument(0));

        service.upsert(req);

        verify(carRepo).save(carCaptor.capture());
        Car saved = carCaptor.getValue();

        assertEquals(10L, saved.getId());
        assertEquals("Toyota", saved.getBrand());
        assertEquals("Corolla", saved.getModel());
        assertEquals(2020, saved.getYear());
        assertEquals("Prata", saved.getColor());
        assertEquals(new BigDecimal("95000.00"), saved.getPrice());
    }

    @Test
    void upsert_shouldUpdateExistingCar_whenFound() {
        Car existing = new Car();
        existing.setId(10L);
        existing.setBrand("Old");

        CarSyncRequest req = new CarSyncRequest(
                10L,
                "Honda",
                "Civic",
                2022,
                "Preto",
                new BigDecimal("120000.00"),
                Instant.now()
        );



        when(carRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(carRepo.save(any(Car.class))).thenAnswer(inv -> inv.getArgument(0));

        service.upsert(req);

        verify(carRepo).save(carCaptor.capture());
        Car saved = carCaptor.getValue();

        // garante que foi o MESMO objeto alterado (update)
        assertSame(existing, saved);

        assertEquals(10L, saved.getId());
        assertEquals("Honda", saved.getBrand());
        assertEquals("Civic", saved.getModel());
        assertEquals(2022, saved.getYear());
        assertEquals("Preto", saved.getColor());
        assertEquals(new BigDecimal("120000.00"), saved.getPrice());
    }
}
