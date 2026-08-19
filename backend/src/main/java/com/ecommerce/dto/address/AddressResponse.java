package com.ecommerce.dto.address;

import com.ecommerce.entity.Address;

/** A saved address as shown in the profile and the checkout address picker. */
public record AddressResponse(
        Long id,
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault,
        String formatted
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefault(),
                address.toSingleLine()
        );
    }
}
