package br.com.fiap.soat7.usecase.services;

import br.com.fiap.soat7.data.domain.dto.CarSyncRequest;

public interface CarSyncService {
    void upsert(CarSyncRequest req);
}