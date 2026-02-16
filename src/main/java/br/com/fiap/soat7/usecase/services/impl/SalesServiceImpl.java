package br.com.fiap.soat7.usecase.services.impl;

import br.com.fiap.soat7.adapter.repositories.CarRepository;
import br.com.fiap.soat7.adapter.repositories.SaleRepository;
import br.com.fiap.soat7.data.domain.Car;
import br.com.fiap.soat7.data.domain.Sale;
import br.com.fiap.soat7.data.domain.dto.CarSoldEvent;
import br.com.fiap.soat7.data.domain.dto.PaymentWebhookRequest;
import br.com.fiap.soat7.data.domain.dto.PurchaseResponse;
import br.com.fiap.soat7.usecase.services.SalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class SalesServiceImpl implements SalesService {

    private final WebClient client;
    private final SaleRepository saleRepo;
    private final CarRepository carRepo;

    @Override
    public List<Car> listAvailable() {

        return carRepo.findBySoldIsFalseOrderByPriceAsc();
    }

    @Override
    public List<Car> listSold() {
        return carRepo.findBySoldIsTrueOrderByPriceAsc();
    }

    @Override
    @Transactional
    public PurchaseResponse purchase(Long carId) {
        Car car = carRepo.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Car não encontrado no serviço de venda"));

        // LOCK no registro de sale por carId (evita 2 compras simultâneas)
        Sale sale = saleRepo.lockByCarId(carId).orElse(null);

        if (sale == null) {
            sale = new Sale();
            sale.setCarId(carId);
            sale.setLockedPrice(car.getPrice());
            sale.setStatus(Sale.Status.AVAILABLE);
        }

        // já vendido
        if (sale.getStatus() == Sale.Status.PAID) {
            throw new IllegalStateException("Car já foi vendido");
        }

        // reservado e ainda válido
        if (sale.getStatus() == Sale.Status.RESERVED
                && sale.getReservedUntil() != null
                && sale.getReservedUntil().isAfter(Instant.now())) {
            throw new IllegalStateException("Car já está reservado");
        }

        // reserva / inicia compra
        sale.setStatus(Sale.Status.RESERVED);
        sale.setReservedUntil(Instant.now().plusSeconds(15 * 60));
        sale.setPaymentCode(UUID.randomUUID().toString());

        Sale saved = saleRepo.save(sale);
        car.setSold(true);
        carRepo.save(car);

        return new PurchaseResponse(
                saved.getId(),
                saved.getCarId(),
                saved.getPaymentCode(),
                saved.getReservedUntil()
        );
    }

    @Override
    @Transactional
    public void handlePaymentWebhook(PaymentWebhookRequest req) {
        String st = req.status() == null ? "" : req.status().toUpperCase();

        Sale sale = saleRepo.findByPaymentCode(req.paymentCode())
                .orElseThrow(() -> new IllegalArgumentException("paymentCode não encontrado"));
        Car car = carRepo.findById(sale.getCarId())
                .orElseThrow(() -> new IllegalArgumentException("Car não encontrado no serviço de venda"));

        // idempotência
        if (sale.getStatus() == Sale.Status.PAID && "PAID".equals(st)) return;
        if (sale.getStatus() == Sale.Status.CANCELED && "CANCELED".equals(st)) return;

        if ("PAID".equals(st)) {
            sale.setStatus(Sale.Status.PAID);
            sale.setBuyerCpf(req.buyerCpf());
            sale.setSoldAt(req.eventAt() != null ? req.eventAt() : Instant.now());
            sale.setReservedUntil(null);

            saleRepo.save(sale);

            notifyCoreCarSold(sale).subscribe();

            return;
        }

        if ("CANCELED".equals(st)) {
            sale.setStatus(Sale.Status.CANCELED);
            sale.setReservedUntil(null);

            car.setSold(false);
            carRepo.save(car);
            saleRepo.save(sale);
            return;
        }

        throw new IllegalArgumentException("status inválido (use PAID ou CANCELED)");
    }

    private Mono<Void> notifyCoreCarSold(Sale sale) {
        CarSoldEvent event = new CarSoldEvent(
                sale.getCarId(), sale.getBuyerCpf(), sale.getSoldAt(), sale.getPaymentCode()
        );
        System.out.println("VIEW vai notificar Core venda do carro " + sale.getCarId() + " -> " + event);
        return client.post()
                .uri("/internal/cars/{carId}/sold", sale.getCarId())
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.info("VIEW notificou Core venda carId={}", sale.getCarId()))
                .doOnError(r -> log.error("VIEW falhou ao notificar Core venda carId={}: {}", sale.getCarId(), r.getMessage()))
                .then();

    }


}
