package br.com.fiap.soat7.adapter.controller;

import br.com.fiap.soat7.data.domain.dto.CarSyncRequest;
import br.com.fiap.soat7.usecase.services.CarSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sync/cars")
@RequiredArgsConstructor
public class CarSyncController {

    private final CarSyncService carSyncService;

    @PostMapping
    public ResponseEntity<Void> upsert(@RequestBody CarSyncRequest req) {
        carSyncService.upsert(req);
        return ResponseEntity.noContent().build(); // 204
    }
}
