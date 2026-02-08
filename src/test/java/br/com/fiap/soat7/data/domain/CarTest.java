package br.com.fiap.soat7.data.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class CarTest {

    @Test
    void construtor_deveCriarCarComSoldFalse() {
        Car car = new Car(
                "VW",
                "Golf",
                2018,
                "Preto",
                new BigDecimal("80000.00")
        );

        assertThat(car.getBrand()).isEqualTo("VW");
        assertThat(car.getModel()).isEqualTo("Golf");
        assertThat(car.getYear()).isEqualTo(2018);
        assertThat(car.getColor()).isEqualTo("Preto");
        assertThat(car.getPrice()).isEqualByComparingTo("80000.00");
        assertThat(car.isSold()).isFalse();
    }

    @Test
    void update_deveAtualizarDadosQuandoNaoVendido() {
        Car car = new Car(
                "VW",
                "Golf",
                2018,
                "Preto",
                new BigDecimal("80000")
        );

        car.update(
                "Audi",
                "A3",
                2020,
                "Branco",
                new BigDecimal("120000")
        );

        assertThat(car.getBrand()).isEqualTo("Audi");
        assertThat(car.getModel()).isEqualTo("A3");
        assertThat(car.getYear()).isEqualTo(2020);
        assertThat(car.getColor()).isEqualTo("Branco");
        assertThat(car.getPrice()).isEqualByComparingTo("120000");
    }

    @Test
    void update_quandoCarVendido_deveLancarExcecao() {
        Car car = new Car(
                "VW",
                "Golf",
                2018,
                "Preto",
                new BigDecimal("80000")
        );

        car.markAsSold();

        assertThatThrownBy(() ->
                car.update(
                        "Audi",
                        "A3",
                        2020,
                        "Branco",
                        new BigDecimal("120000")
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já vendido");
    }

    @Test
    void markAsSold_deveMarcarComoVendido() {
        Car car = new Car(
                "VW",
                "Golf",
                2018,
                "Preto",
                new BigDecimal("80000")
        );

        car.markAsSold();

        assertThat(car.isSold()).isTrue();
    }

    @Test
    void markAsSold_duasVezes_deveLancarExcecao() {
        Car car = new Car(
                "VW",
                "Golf",
                2018,
                "Preto",
                new BigDecimal("80000")
        );

        car.markAsSold();

        assertThatThrownBy(car::markAsSold)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já vendido");
    }
}
