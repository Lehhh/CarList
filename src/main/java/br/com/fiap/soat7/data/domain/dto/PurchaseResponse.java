package br.com.fiap.soat7.data.domain.dto;

import java.time.Instant;

public record PurchaseResponse(
        Long saleId,
        Long carId,
        String paymentCode,
        Instant reservedUntil
) {}
