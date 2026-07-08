package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.product.Product;

import java.util.List;

public interface FindProductUseCase {

    Product findById(Long id);
    Product findBySku(String sku);
    List<Product> findAll();
    List<Product> findByCategoryId(Long categoryId);
}
