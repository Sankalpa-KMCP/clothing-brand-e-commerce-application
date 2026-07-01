package com.clothingbrand.ecommerce.domain.address;

import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    public CustomerAddressController(CustomerAddressService customerAddressService) {
        this.customerAddressService = customerAddressService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerAddressResponseDto>> getMyAddresses(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<CustomerAddressResponseDto> response = customerAddressService.getMyAddresses(userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CustomerAddressResponseDto> createMyAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CustomerAddressRequestDto request) {
        CustomerAddressResponseDto response = customerAddressService.createMyAddress(userDetails.getUser().getId(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<CustomerAddressResponseDto> getMyAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long addressId) {
        CustomerAddressResponseDto response = customerAddressService.getMyAddress(userDetails.getUser().getId(), addressId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<CustomerAddressResponseDto> updateMyAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long addressId,
            @Valid @RequestBody CustomerAddressRequestDto request) {
        CustomerAddressResponseDto response = customerAddressService.updateMyAddress(userDetails.getUser().getId(), addressId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteMyAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long addressId) {
        customerAddressService.deleteMyAddress(userDetails.getUser().getId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<CustomerAddressResponseDto> setMyDefaultAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long addressId) {
        CustomerAddressResponseDto response = customerAddressService.setMyDefaultAddress(userDetails.getUser().getId(), addressId);
        return ResponseEntity.ok(response);
    }
}
