package com.ecommerce.controller;

import com.ecommerce.dto.common.MessageResponse;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.product.ProductFilterRequest;
import com.ecommerce.dto.product.ProductRequest;
import com.ecommerce.dto.product.ProductResponse;
import com.ecommerce.dto.product.StockUpdateRequest;
import com.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Product management.
 *
 * <p>Every route sits under {@code /api/admin/**}, which {@code SecurityConfig} gates
 * behind {@code hasRole("ADMIN")} with a single rule. That is why there is no per-method
 * security annotation here - and why a new admin endpoint cannot ship unprotected by
 * accident.
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Tag(name = "Admin - Products", description = "Create, edit, delete and restock products (ROLE_ADMIN)")
public class AdminProductController {

    private final ProductService productService;

    /** Unlike the public listing, this includes deactivated products. */
    @GetMapping
    @Operation(summary = "List all products, including deactivated ones")
    public ResponseEntity<PagedResponse<ProductResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var filter = new ProductFilterRequest(
                q, null, categoryId, brand, minPrice, maxPrice, null, null, sort);

        return ResponseEntity.ok(productService.search(filter, page, size, true));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one product, active or not")
    public ResponseEntity<ProductResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getByIdForAdmin(id));
    }

    @PostMapping
    @Operation(summary = "Create a product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Set a product's stock level")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable Long id,
                                                       @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(productService.updateStock(id, request.stock()));
    }

    /**
     * Deletes a product, or deactivates it when it appears in existing orders.
     *
     * <p>The response says which happened, so the admin UI can explain why a "deleted"
     * product is still visible in order history rather than looking broken.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product (soft-deletes if it appears in past orders)")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        boolean deactivated = productService.delete(id);
        return ResponseEntity.ok(MessageResponse.of(deactivated
                ? "This product appears in existing orders, so it was deactivated and hidden from the store rather than deleted."
                : "Product deleted."));
    }
}
