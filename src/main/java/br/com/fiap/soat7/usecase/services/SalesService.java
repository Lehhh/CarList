package br.com.fiap.soat7.usecase.services;

import br.com.fiap.soat7.data.domain.Sale;
import br.com.fiap.soat7.data.domain.dto.PaymentWebhookRequest;
import br.com.fiap.soat7.data.domain.dto.PurchaseResponse;

import java.util.List;

public interface SalesService {

    List<Sale> listAvailable();
    List<Sale> listSold();
    PurchaseResponse purchase(Long carId);
    void handlePaymentWebhook(PaymentWebhookRequest req);
}
