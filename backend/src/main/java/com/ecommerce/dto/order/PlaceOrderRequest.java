package com.ecommerce.dto.order;

import com.ecommerce.dto.address.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
        Long addressId,

        @Valid
        AddressRequest newAddress,

        Boolean saveNewAddress,

        @Size(max = 500, message = "Order note must not exceed 500 characters")
        String note,

        @Size(max = 32, message = "Coupon code must not exceed 32 characters")
        String couponCode
) {
    public boolean hasExactlyOneAddressSource() {
        return (addressId != null) ^ (newAddress != null);
    }
}
