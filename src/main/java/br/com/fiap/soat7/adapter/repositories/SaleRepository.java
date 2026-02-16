package br.com.fiap.soat7.adapter.repositories;

import br.com.fiap.soat7.data.domain.Sale;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByStatusOrderByLockedPriceAsc(Sale.Status status);

    Optional<Sale> findByPaymentCode(String paymentCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.carId = :carId")
    Optional<Sale> lockByCarId(@Param("carId") Long carId);
}
