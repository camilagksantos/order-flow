package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.product.Product;

public interface UpdateProductUseCase {

    Product updateProduct(Long id, Product product);
}
