package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.product.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product product);
    Optional<Product> findById(Long id);
    Optional<Product> findBySku(String sku);
    List<Product> findAll();
    List<Product> findByCategoryId(Long categoryId);
    void deleteById(Long id);
}
