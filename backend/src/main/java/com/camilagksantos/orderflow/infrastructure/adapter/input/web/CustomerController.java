package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.application.dto.request.CreateAddressRequest;
import com.camilagksantos.orderflow.application.dto.request.RegisterCustomerRequest;
import com.camilagksantos.orderflow.application.dto.request.UpdateAddressRequest;
import com.camilagksantos.orderflow.application.dto.response.AddressResponse;
import com.camilagksantos.orderflow.application.dto.response.CustomerResponse;
import com.camilagksantos.orderflow.application.mapper.AddressMapper;
import com.camilagksantos.orderflow.application.mapper.CustomerMapper;
import com.camilagksantos.orderflow.application.port.input.AddAddressUseCase;
import com.camilagksantos.orderflow.application.port.input.FindCustomerUseCase;
import com.camilagksantos.orderflow.application.port.input.RegisterCustomerUseCase;
import com.camilagksantos.orderflow.application.port.input.RemoveAddressUseCase;
import com.camilagksantos.orderflow.application.port.input.SetDefaultAddressUseCase;
import com.camilagksantos.orderflow.application.port.input.UpdateAddressUseCase;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.infrastructure.config.security.SecurityUtils;
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
    private final AddAddressUseCase addAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;
    private final RemoveAddressUseCase removeAddressUseCase;
    private final CustomerMapper customerMapper;
    private final AddressMapper addressMapper;

    @PostMapping
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        Customer customer = customerMapper.toDomain(request);
        customer.setStatus(CustomerStatus.ACTIVE);
        Customer created = registerCustomerUseCase.registerCustomer(customer, request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(customerMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> findById(@PathVariable Long id) {
        SecurityUtils.requireCustomerOrAdminAccess(id);
        return ResponseEntity.ok(customerMapper.toResponse(findCustomerUseCase.findCustomerById(id)));
    }

    @PostMapping("/{customerId}/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable Long customerId,
            @Valid @RequestBody CreateAddressRequest request) {
        SecurityUtils.requireCustomerAccess(customerId);
        var created = addAddressUseCase.addAddress(customerId, addressMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(addressMapper.toResponse(created));
    }

    @PutMapping("/{customerId}/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long customerId,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        SecurityUtils.requireCustomerAccess(customerId);
        var updated = updateAddressUseCase.updateAddress(customerId, addressId, addressMapper.toDomain(request));
        return ResponseEntity.ok(addressMapper.toResponse(updated));
    }

    @PatchMapping("/{customerId}/addresses/{addressId}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable Long customerId,
            @PathVariable Long addressId) {
        SecurityUtils.requireCustomerAccess(customerId);
        var address = setDefaultAddressUseCase.setDefaultAddress(customerId, addressId);
        return ResponseEntity.ok(addressMapper.toResponse(address));
    }

    @DeleteMapping("/{customerId}/addresses/{addressId}")
    public ResponseEntity<Void> removeAddress(
            @PathVariable Long customerId,
            @PathVariable Long addressId) {
        SecurityUtils.requireCustomerAccess(customerId);
        removeAddressUseCase.removeAddress(customerId, addressId);
        return ResponseEntity.noContent().build();
    }
}