package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.CreateProductUseCase;
import com.camilagksantos.orderflow.application.port.input.DeleteProductUseCase;
import com.camilagksantos.orderflow.application.port.input.FindProductUseCase;
import com.camilagksantos.orderflow.application.port.input.UpdateProductUseCase;
import com.camilagksantos.orderflow.application.port.output.ProductRepositoryPort;
import com.camilagksantos.orderflow.domain.exception.ProductNotFoundException;
import com.camilagksantos.orderflow.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService implements CreateProductUseCase, FindProductUseCase, UpdateProductUseCase, DeleteProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    public Product createProduct(Product product) {
        return productRepositoryPort.save(product);
    }

    @Override
    public Product findProductById(Long id) {
        return productRepositoryPort.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public Product findProductBySku(String sku) {
        return productRepositoryPort.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(-1L));
    }

    @Override
    public List<Product> findAllProducts() {
        return productRepositoryPort.findAll();
    }

    @Override
    public List<Product> findProductsByCategoryId(Long categoryId) {
        return productRepositoryPort.findByCategoryId(categoryId);
    }

    @Override
    public Product updateProduct(Long id, Product product) {
        findProductById(id);
        return productRepositoryPort.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        findProductById(id);
        productRepositoryPort.deleteById(id);
    }
}