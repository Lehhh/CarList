package br.com.fiap.soat7.usecase.services;

import br.com.fiap.soat7.data.domain.Car;
import br.com.fiap.soat7.data.domain.dto.PaymentWebhookRequest;
import br.com.fiap.soat7.data.domain.dto.PurchaseResponse;

import java.util.List;

public interface SalesService {

    List<Car> listAvailable();
    List<Car> listSold();
    List<Car> listReserved();
    PurchaseResponse reserved(Long carId);
    void handlePaymentWebhook(PaymentWebhookRequest req);
}
