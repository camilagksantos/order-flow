package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.product.Product;

public interface UpdateProductUseCase {

    Product execute(Long id, Product product);
}
