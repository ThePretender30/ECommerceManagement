package com.ecommerce.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import com.ecommerce.dto.address.AddressRequest;

/**
 * Checkout payload for {@code POST /api/orders}.
 *
 * <p>The customer either picks a saved address ({@code addressId}) or types a new one
 * ({@code newAddress}); exactly one must be supplied, which {@code OrderService} enforces.
 *
 * <p>There are no line items here. The order is always built from the server-side cart, so
 * the client cannot submit quantities or prices that were never validated against stock.
 *
 * @param saveNewAddress when a new address is entered, also save it to the address book
 */
public record PlaceOrderRequest(

        Long addressId,

        @Valid
        AddressRequest newAddress,

        Boolean saveNewAddress,

        @Size(max = 500, message = "Order note must not exceed 500 characters")
        String note
) {
    /** True when the caller supplied exactly one of the two address options. */
    public boolean hasExactlyOneAddressSource() {
        return (addressId != null) ^ (newAddress != null);
    }
}
