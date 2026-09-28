package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.application.dto.request.ProcessPaymentRequest;
import com.camilagksantos.orderflow.application.dto.response.PaymentResponse;
import com.camilagksantos.orderflow.application.mapper.PaymentMapper;
import com.camilagksantos.orderflow.application.port.input.FindOrderUseCase;
import com.camilagksantos.orderflow.application.port.input.ProcessPaymentUseCase;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.payment.Payment;
import com.camilagksantos.orderflow.infrastructure.config.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final FindOrderUseCase findOrderUseCase;
    private final PaymentMapper paymentMapper;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        ShopOrder order = findOrderUseCase.findOrderById(request.orderId());
        SecurityUtils.requireOrderAccess(order.getCustomerId());

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .method(request.method())
                .cardLastFour(request.cardLastFour())
                .cardBrand(request.cardBrand())
                .mbwayPhone(request.mbwayPhone())
                .mbEntity(request.mbEntity())
                .mbReference(request.mbReference())
                .build();

        Payment processed = processPaymentUseCase.processPayment(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMapper.toResponse(processed));
    }
}