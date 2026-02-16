package br.com.fiap.soat7.adapter.controller;
import br.com.fiap.soat7.data.domain.Car;
import br.com.fiap.soat7.data.domain.dto.PaymentWebhookRequest;
import br.com.fiap.soat7.data.domain.dto.PurchaseRequest;
import br.com.fiap.soat7.data.domain.dto.PurchaseResponse;
import br.com.fiap.soat7.usecase.services.SalesService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/1/sales")
@SecurityRequirement(name = "bearerAuth")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    /**
     * Listagem de veículos à venda, ordenada por preço (asc)
     */
    @GetMapping("/available")
    public ResponseEntity<List<Car>> listAvailable() {
        return ResponseEntity.ok(salesService.listAvailable());
    }

    /**
     * Listagem de veículos vendidos, ordenada por preço (asc)
     */
    @GetMapping("/sold")
    public ResponseEntity<List<Car>> listSold() {
        return ResponseEntity.ok(salesService.listSold());
    }


    @GetMapping("/reserved")
    public ResponseEntity<List<Car>> listReserved() {
        return ResponseEntity.ok(salesService.listReserved());
    }
    /**
     * Inicia compra/reserva e gera paymentCode
     */
    @PostMapping("/reserved")
    public ResponseEntity<PurchaseResponse> reserved(@RequestBody PurchaseRequest req) {
        return ResponseEntity.ok(salesService.reserved(req.carId()));
    }

    /**
     * Webhook do pagamento: pago/cancelado pelo paymentCode
     * Normalmente este endpoint NÃO exige JWT do usuário final.
     * (Você pode proteger com header secreto)
     */
    @PostMapping("/payments/webhook")
    public ResponseEntity<Void> webhook(@RequestBody PaymentWebhookRequest req) {
        salesService.handlePaymentWebhook(req);
        return ResponseEntity.noContent().build(); // 204
    }
}
