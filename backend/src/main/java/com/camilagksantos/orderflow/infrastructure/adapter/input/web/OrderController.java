package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.application.dto.request.CancelOrderRequest;
import com.camilagksantos.orderflow.application.dto.request.UpdateOrderStatusRequest;
import com.camilagksantos.orderflow.application.dto.response.OrderResponse;
import com.camilagksantos.orderflow.application.mapper.OrderMapper;
import com.camilagksantos.orderflow.application.port.input.CancelOrderUseCase;
import com.camilagksantos.orderflow.application.port.input.FindOrderUseCase;
import com.camilagksantos.orderflow.application.port.input.UpdateOrderStatusUseCase;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.infrastructure.config.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final FindOrderUseCase findOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final OrderMapper orderMapper;

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable String id) {
        ShopOrder order = findOrderUseCase.findOrderById(id);
        SecurityUtils.requireOrderAccess(order.getCustomerId());
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> findByOrderNumber(@PathVariable String orderNumber) {
        ShopOrder order = findOrderUseCase.findOrderByNumber(orderNumber);
        SecurityUtils.requireOrderAccess(order.getCustomerId());
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> findByCustomerId(@PathVariable Long customerId) {
        SecurityUtils.requireCustomerAccess(customerId);
        List<OrderResponse> orders = findOrderUseCase.findOrdersByCustomerId(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderMapper.toResponse(
                updateOrderStatusUseCase.updateOrderStatus(id, request.status())));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable String id,
            @Valid @RequestBody CancelOrderRequest request) {
        ShopOrder order = findOrderUseCase.findOrderById(id);
        SecurityUtils.requireOrderAccess(order.getCustomerId());
        return ResponseEntity.ok(orderMapper.toResponse(
                cancelOrderUseCase.cancelOrder(id, request.reason())));
    }
}