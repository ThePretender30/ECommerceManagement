package com.ecommerce.controller;

import com.ecommerce.dto.address.AddressRequest;
import com.ecommerce.dto.address.AddressResponse;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The signed-in customer's saved delivery addresses. */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Manage your delivery addresses")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "List your saved addresses, default first")
    public ResponseEntity<List<AddressResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(addressService.getUserAddresses(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one of your addresses")
    public ResponseEntity<AddressResponse> get(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long id) {
        return ResponseEntity.ok(addressService.getById(principal.getId(), id));
    }

    @PostMapping
    @Operation(summary = "Save a new address (the first one becomes your default)")
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of your addresses")
    public ResponseEntity<AddressResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(principal.getId(), id, request));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Make this your default delivery address")
    public ResponseEntity<AddressResponse> setDefault(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(addressService.setDefault(principal.getId(), id));
    }

    /** Safe at any time - past orders keep their own copy of the delivery address. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one of your addresses")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long id) {
        addressService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
