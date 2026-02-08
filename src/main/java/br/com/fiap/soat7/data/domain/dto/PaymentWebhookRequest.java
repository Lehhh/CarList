package br.com.fiap.soat7.data.domain.dto;

import java.time.Instant;

public record PaymentWebhookRequest(
        String paymentCode,
        String status,   // "PAID" ou "CANCELED"
        String buyerCpf,
        Instant eventAt
) {}