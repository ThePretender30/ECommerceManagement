package com.ecommerce.service;

import com.ecommerce.dto.address.AddressRequest;
import com.ecommerce.dto.address.AddressResponse;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.User;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The customer's address book.
 *
 * <p>Like the cart, every lookup is scoped to the authenticated user id, so an address id
 * from another account is simply "not found" rather than accessible.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId)
                .stream().map(AddressResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getById(Long userId, Long addressId) {
        return AddressResponse.from(findOwnedAddressOrThrow(userId, addressId));
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // The very first address a user saves becomes their default automatically.
        boolean isFirstAddress = addressRepository.countByUserId(userId) == 0;
        boolean makeDefault = isFirstAddress || Boolean.TRUE.equals(request.isDefault());

        if (makeDefault) {
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.fullName().trim())
                .phone(request.phone().trim())
                .line1(request.line1().trim())
                .line2(trimToNull(request.line2()))
                .city(request.city().trim())
                .state(request.state().trim())
                .postalCode(request.postalCode().trim())
                .country(request.country() == null || request.country().isBlank()
                        ? "India" : request.country().trim())
                .isDefault(makeDefault)
                .build();

        Address saved = addressRepository.save(address);
        log.debug("User {} saved address {}", userId, saved.getId());
        return AddressResponse.from(saved);
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        Address address = findOwnedAddressOrThrow(userId, addressId);

        if (Boolean.TRUE.equals(request.isDefault()) && !address.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
            address.setDefault(true);
        }

        address.setFullName(request.fullName().trim());
        address.setPhone(request.phone().trim());
        address.setLine1(request.line1().trim());
        address.setLine2(trimToNull(request.line2()));
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
        if (request.country() != null && !request.country().isBlank()) {
            address.setCountry(request.country().trim());
        }

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse setDefault(Long userId, Long addressId) {
        Address address = findOwnedAddressOrThrow(userId, addressId);
        addressRepository.clearDefaultForUser(userId);
        address.setDefault(true);
        return AddressResponse.from(addressRepository.save(address));
    }

    /**
     * Deletes an address.
     *
     * <p>Safe to do at any time: orders keep their own copy of the delivery address, so
     * removing this row cannot alter where a past order was sent.
     */
    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = findOwnedAddressOrThrow(userId, addressId);
        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);

        // Promote another address so the user always has a default to check out with.
        if (wasDefault) {
            addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        addressRepository.save(next);
                    });
        }
        log.debug("User {} deleted address {}", userId, addressId);
    }

    /** Ownership-scoped lookup used by this service and by checkout. */
    @Transactional(readOnly = true)
    public Address findOwnedAddressOrThrow(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
