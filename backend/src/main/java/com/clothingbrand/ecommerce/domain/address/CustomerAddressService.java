package com.clothingbrand.ecommerce.domain.address;

import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import com.clothingbrand.ecommerce.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final UserRepository userRepository;

    public CustomerAddressService(CustomerAddressRepository customerAddressRepository, UserRepository userRepository) {
        this.customerAddressRepository = customerAddressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerAddressResponseDto> getMyAddresses(Long authenticatedUserId) {
        return customerAddressRepository.findAllByUserIdOrdered(authenticatedUserId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerAddressResponseDto createMyAddress(Long authenticatedUserId, CustomerAddressRequestDto request) {
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomerAddress address = new CustomerAddress();
        address.setUser(user);
        address.setIsDefault(false);
        updateEntityFromDto(address, request);

        CustomerAddress saved = customerAddressRepository.save(address);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public CustomerAddressResponseDto getMyAddress(Long authenticatedUserId, Long addressId) {
        CustomerAddress address = getAddressSafely(addressId, authenticatedUserId);
        return mapToDto(address);
    }

    @Transactional
    public CustomerAddressResponseDto updateMyAddress(Long authenticatedUserId, Long addressId, CustomerAddressRequestDto request) {
        CustomerAddress address = getAddressSafely(addressId, authenticatedUserId);
        updateEntityFromDto(address, request);
        CustomerAddress saved = customerAddressRepository.save(address);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteMyAddress(Long authenticatedUserId, Long addressId) {
        CustomerAddress address = getAddressSafely(addressId, authenticatedUserId);
        customerAddressRepository.delete(address);
    }

    @Transactional
    public CustomerAddressResponseDto setMyDefaultAddress(Long authenticatedUserId, Long addressId) {
        CustomerAddress address = getAddressSafely(addressId, authenticatedUserId);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            return mapToDto(address);
        }

        // Clear existing defaults, which also clears the persistence context
        customerAddressRepository.clearDefaultAddressesForUser(authenticatedUserId);

        // Re-fetch to ensure we are working with a managed entity
        address = getAddressSafely(addressId, authenticatedUserId);
        address.setIsDefault(true);
        CustomerAddress saved = customerAddressRepository.save(address);
        return mapToDto(saved);
    }

    private CustomerAddress getAddressSafely(Long addressId, Long userId) {
        if (addressId == null) {
            throw new ResourceNotFoundException("Address not found");
        }
        return customerAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private void updateEntityFromDto(CustomerAddress address, CustomerAddressRequestDto request) {
        address.setLabel(normalizeString(request.label()));
        address.setRecipientName(normalizeString(request.recipientName()));
        address.setPhoneNumber(normalizeString(request.phoneNumber()));
        address.setAddressLine1(normalizeString(request.addressLine1()));
        address.setAddressLine2(normalizeString(request.addressLine2()));
        address.setCity(normalizeString(request.city()));
        address.setRegion(normalizeString(request.region()));
        address.setPostalCode(normalizeString(request.postalCode()));
        address.setCountry(normalizeString(request.country()));
    }

    private String normalizeString(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CustomerAddressResponseDto mapToDto(CustomerAddress address) {
        return new CustomerAddressResponseDto(
                address.getId(),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getRegion(),
                address.getPostalCode(),
                address.getCountry(),
                address.getIsDefault()
        );
    }
}
