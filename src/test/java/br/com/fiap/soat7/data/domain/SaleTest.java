package br.com.fiap.soat7.data.domain;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class SaleTest {

    @Test
    void devePermitirSetarEObterCampos() {
        Sale sale = new Sale();

        sale.setId(1L);
        sale.setCarId(55L);
        sale.setStatus(Sale.Status.RESERVED);
        sale.setLockedPrice(new BigDecimal("70000.00"));
        sale.setReservedUntil(Instant.parse("2026-02-08T10:00:00Z"));
        sale.setPaymentCode("PAY-123");
        sale.setBuyerCpf("12345678901");
        sale.setSoldAt(Instant.parse("2026-02-08T10:05:00Z"));
        sale.setVersion(2L);

        assertThat(sale.getId()).isEqualTo(1L);
        assertThat(sale.getCarId()).isEqualTo(55L);
        assertThat(sale.getStatus()).isEqualTo(Sale.Status.RESERVED);
        assertThat(sale.getLockedPrice()).isEqualByComparingTo("70000.00");
        assertThat(sale.getReservedUntil()).isEqualTo(Instant.parse("2026-02-08T10:00:00Z"));
        assertThat(sale.getPaymentCode()).isEqualTo("PAY-123");
        assertThat(sale.getBuyerCpf()).isEqualTo("12345678901");
        assertThat(sale.getSoldAt()).isEqualTo(Instant.parse("2026-02-08T10:05:00Z"));
        assertThat(sale.getVersion()).isEqualTo(2L);
    }

    @Test
    void status_deveConterValoresEsperados() {
        assertThat(Sale.Status.values())
                .containsExactly(
                        Sale.Status.AVAILABLE,
                        Sale.Status.RESERVED,
                        Sale.Status.PAID,
                        Sale.Status.CANCELED
                );
    }

    @Test
    void deveTerAnotacoesJpaBasicas() throws Exception {
        // @Entity
        assertThat(Sale.class.isAnnotationPresent(Entity.class)).isTrue();

        // @Table(name="sales" ...)
        Table table = Sale.class.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("sales");
        assertThat(table.indexes()).hasSize(2);

        assertThat(table.indexes()[0].name()).isEqualTo("idx_sale_car");
        assertThat(table.indexes()[0].columnList()).isEqualTo("car_id");
        assertThat(table.indexes()[0].unique()).isTrue();

        assertThat(table.indexes()[1].name()).isEqualTo("idx_sale_payment");
        assertThat(table.indexes()[1].columnList()).isEqualTo("payment_code");
        assertThat(table.indexes()[1].unique()).isTrue();

        // Campos com @Column e @Version (checks simples)
        Field carId = Sale.class.getDeclaredField("carId");
        Column carIdCol = carId.getAnnotation(Column.class);
        assertThat(carIdCol).isNotNull();
        assertThat(carIdCol.name()).isEqualTo("car_id");
        assertThat(carIdCol.nullable()).isFalse();
        assertThat(carIdCol.unique()).isTrue();

        Field paymentCode = Sale.class.getDeclaredField("paymentCode");
        Column payCol = paymentCode.getAnnotation(Column.class);
        assertThat(payCol).isNotNull();
        assertThat(payCol.name()).isEqualTo("payment_code");
        assertThat(payCol.unique()).isTrue();

        Field version = Sale.class.getDeclaredField("version");
        assertThat(version.isAnnotationPresent(Version.class)).isTrue();
    }
}
