package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.product.Product;

public interface CreateProductUseCase {

    Product createProduct(Product product);
}
