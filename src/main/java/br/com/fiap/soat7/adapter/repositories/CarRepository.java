package br.com.fiap.soat7.adapter.repositories;

import br.com.fiap.soat7.data.domain.Car;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findBySoldIsFalseOrderByPriceAsc();
    List<Car> findBySoldIsTrueOrderByPriceAsc();
}
