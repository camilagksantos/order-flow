package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.application.dto.request.CreateProductRequest;
import com.camilagksantos.orderflow.application.dto.request.UpdateProductRequest;
import com.camilagksantos.orderflow.application.dto.response.ProductResponse;
import com.camilagksantos.orderflow.application.mapper.ProductMapper;
import com.camilagksantos.orderflow.application.port.input.CreateProductUseCase;
import com.camilagksantos.orderflow.application.port.input.DeleteProductUseCase;
import com.camilagksantos.orderflow.application.port.input.FindProductUseCase;
import com.camilagksantos.orderflow.application.port.input.UpdateProductUseCase;
import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.domain.product.Product;
import com.camilagksantos.orderflow.domain.product.ProductStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final FindProductUseCase findProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final ProductMapper productMapper;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product product = productMapper.toDomain(request);
        product.setCategory(Category.builder().id(request.categoryId()).build());
        product.setStatus(ProductStatus.ACTIVE);
        Product created = createProductUseCase.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {
        List<ProductResponse> products = findProductUseCase.findAllProducts().stream()
                .map(productMapper::toResponse)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productMapper.toResponse(findProductUseCase.findProductById(id)));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> findBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productMapper.toResponse(findProductUseCase.findProductBySku(sku)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> findByCategoryId(@PathVariable Long categoryId) {
        List<ProductResponse> products = findProductUseCase.findProductsByCategoryId(categoryId).stream()
                .map(productMapper::toResponse)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        Product product = productMapper.toDomain(request);
        product.setCategory(Category.builder().id(request.categoryId()).build());
        Product updated = updateProductUseCase.updateProduct(id, product);
        return ResponseEntity.ok(productMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteProductUseCase.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}