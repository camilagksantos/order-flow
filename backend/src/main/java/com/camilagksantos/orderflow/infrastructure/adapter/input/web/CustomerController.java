package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.application.dto.request.RegisterCustomerRequest;
import com.camilagksantos.orderflow.application.dto.response.CustomerResponse;
import com.camilagksantos.orderflow.application.mapper.CustomerMapper;
import com.camilagksantos.orderflow.application.port.input.FindCustomerUseCase;
import com.camilagksantos.orderflow.application.port.input.RegisterCustomerUseCase;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final FindCustomerUseCase findCustomerUseCase;
    private final CustomerMapper customerMapper;

    @PostMapping
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        Customer customer = customerMapper.toDomain(request);
        customer.setStatus(CustomerStatus.ACTIVE);
        Customer created = registerCustomerUseCase.registerCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(customerMapper.toResponse(findCustomerUseCase.findCustomerById(id)));
    }
}