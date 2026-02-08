package br.com.fiap.soat7.data.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "sales",
        indexes = {
                @Index(name = "idx_sale_car", columnList = "car_id", unique = true),
                @Index(name = "idx_sale_payment", columnList = "payment_code", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Sale {

    public enum Status {
        AVAILABLE,
        RESERVED,
        PAID,
        CANCELED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID do carro replicado do Core
     * Não usamos relacionamento JPA aqui para evitar acoplamento
     */
    @Column(name = "car_id", nullable = false, unique = true)
    private Long carId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /**
     * Preço travado no momento da reserva
     */
    @Column(name = "locked_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal lockedPrice;

    /**
     * Expiração da reserva
     */
    @Column(name = "reserved_until")
    private Instant reservedUntil;

    /**
     * Código enviado ao gateway de pagamento
     */
    @Column(name = "payment_code", unique = true)
    private String paymentCode;

    /**
     * Preenchido quando pagamento = PAID
     */
    @Column(name = "buyer_cpf", length = 11)
    private String buyerCpf;

    @Column(name = "sold_at")
    private Instant soldAt;


    /**
     * Evita conflitos concorrentes
     */
    @Version
    private Long version;
}
