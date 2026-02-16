package br.com.fiap.soat7.data.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "car_view")
@Getter
@Setter
@NoArgsConstructor
public class Car {

    @Id
    private Long id;

    @Column(nullable = false, length = 60)
    private String brand;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(nullable = false, name = "car_year")
    private Integer year;

    @Column(nullable = false, length = 30)
    private String color;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    private boolean sold;

    @Column(nullable = false)
    private Instant updatedAt;

    public Car(String brand, String model, Integer year, String color, BigDecimal price) {
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.color = color;
        this.price = price;
        this.sold = false;
        this.updatedAt = Instant.now();
    }

    public void update(String brand, String model, Integer year, String color, BigDecimal price) {
        if (this.sold) {
            throw new IllegalStateException("Veículo já vendido; não é permitido editar.");
        }
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.color = color;
        this.price = price;
        this.updatedAt = Instant.now();

    }

    public void markAsSold() {
        if (this.sold) {
            throw new IllegalStateException("Veículo já vendido.");
        }
        this.sold = true;
    }
}
