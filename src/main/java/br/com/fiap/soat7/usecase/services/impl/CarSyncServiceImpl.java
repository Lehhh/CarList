package br.com.fiap.soat7.usecase.services.impl;

import br.com.fiap.soat7.adapter.repositories.CarRepository;
import br.com.fiap.soat7.data.domain.Car;
import br.com.fiap.soat7.data.domain.dto.CarSyncRequest;
import br.com.fiap.soat7.usecase.services.CarSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CarSyncServiceImpl implements CarSyncService {

    private final CarRepository carRepo;

    @Override
    @Transactional
    public void upsert(CarSyncRequest req) {

        Car car = carRepo.findById(req.id())
                .orElseGet(Car::new);

        car.setId(req.id());
        car.setBrand(req.brand());
        car.setModel(req.model());
        car.setYear(req.year());
        car.setColor(req.color());
        car.setPrice(req.price());
        car.setUpdatedAt(Instant.now());
        car.setSold(false);
        carRepo.save(car);
    }
}
