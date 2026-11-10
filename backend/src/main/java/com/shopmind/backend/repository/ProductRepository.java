
package com.shopmind.backend.repository;

import com.shopmind.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByBrandIgnoreCase(String brand);

    List<Product> findByPriceBetween(BigDecimal minPrice,BigDecimal maxPrice);

    boolean existsByCategoryId(Long categoryId);
}
