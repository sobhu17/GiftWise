package com.giftwise.product.controller;

import com.giftwise.product.dto.ProductRequest;
import com.giftwise.product.dto.ProductResponse;
import com.giftwise.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    /**
     * Create a new product in the authenticated business's catalog.
     *
     * @param request : validated product fields from the request body
     * @return 201 Created with the newly created product
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid ProductRequest request) {
        UUID businessId = getAuthenticatedBusinessId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request, businessId));
    }

    /**
     * List every product — active and inactive — owned by the authenticated business.
     * Used by the catalog management dashboard.
     *
     * @return 200 OK with the full product list for this business
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> listProducts() {
        UUID businessId = getAuthenticatedBusinessId();
        return ResponseEntity.ok(productService.listProducts(businessId));
    }

    /**
     * Fetch a single product by id, scoped to the authenticated business.
     *
     * @param id : id of the product to fetch, taken from the path
     * @return 200 OK with the matching product
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        UUID businessId = getAuthenticatedBusinessId();
        return ResponseEntity.ok(productService.getProduct(id , businessId));
    }

    /**
     * Replace an existing product's fields and trigger regeneration of its embedding.
     *
     * @param id      : id of the product to update, taken from the path
     * @param request : validated replacement fields from the request body
     * @return 200 OK with the updated product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id , @RequestBody @Valid ProductRequest request) {
        UUID businessId = getAuthenticatedBusinessId();
        return ResponseEntity.ok(productService.updateProduct(id , request , businessId));
    }

    /**
     * Soft-delete a product (sets {@code is_active = false}). The row is kept, never removed.
     *
     * @param id : id of the product to deactivate, taken from the path
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        UUID businessId = getAuthenticatedBusinessId();
        productService.deleteProduct(id , businessId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Read the businessId that {@link com.giftwise.product.security.JwtAuthFilter} placed in the
     * security context as the authentication principal.
     * <p>
     * This is how every endpoint here gets its businessId — never from the request body or a
     * path/query parameter, so a caller cannot act on behalf of a different business.
     *
     * @return id of the business that owns the current authenticated request
     */
    private UUID getAuthenticatedBusinessId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()
        );
    }
}
