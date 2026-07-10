package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.product.Product;

import java.util.List;

public interface FindProductUseCase {

    Product findProductById(Long id);
    Product findProductBySku(String sku);
    List<Product> findAllProducts();
    List<Product> findProductsByCategoryId(Long categoryId);
}
