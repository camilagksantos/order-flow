package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.application.dto.request.AddToCartRequest;
import com.camilagksantos.orderflow.application.dto.request.CheckoutRequest;
import com.camilagksantos.orderflow.application.dto.response.CartResponse;
import com.camilagksantos.orderflow.application.dto.response.OrderResponse;
import com.camilagksantos.orderflow.application.mapper.CartMapper;
import com.camilagksantos.orderflow.application.mapper.OrderMapper;
import com.camilagksantos.orderflow.application.port.input.AddToCartUseCase;
import com.camilagksantos.orderflow.application.port.input.CheckoutUseCase;
import com.camilagksantos.orderflow.application.port.input.FindCartUseCase;
import com.camilagksantos.orderflow.application.port.input.RemoveFromCartUseCase;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.shared.Money;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final AddToCartUseCase addToCartUseCase;
    private final RemoveFromCartUseCase removeFromCartUseCase;
    private final FindCartUseCase findCartUseCase;
    private final CheckoutUseCase checkoutUseCase;
    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<CartResponse> findByCustomerId(@PathVariable Long customerId) {
        Cart cart = findCartUseCase.findCartByCustomerId(customerId);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @PostMapping("/customer/{customerId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable Long customerId,
            @Valid @RequestBody AddToCartRequest request) {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(request.productId())
                .quantity(request.quantity())
                .build();
        Cart cart = addToCartUseCase.addToCart(customerId, item);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartMapper.toResponse(cart));
    }

    @DeleteMapping("/customer/{customerId}/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long customerId,
            @PathVariable String itemId) {
        Cart cart = removeFromCartUseCase.removeFromCart(customerId, itemId);
        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @PostMapping("/customer/{customerId}/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @PathVariable Long customerId,
            @Valid @RequestBody CheckoutRequest request) {
        ShopOrder order = checkoutUseCase.checkout(customerId, request.idempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponse(order));
    }
}