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

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid ProductRequest request) {
        UUID businessId = getAuthenticatedBusinessId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request, businessId));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> listProducts() {
        UUID businessId = getAuthenticatedBusinessId();
        return ResponseEntity.ok(productService.listProducts(businessId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        UUID businessId = getAuthenticatedBusinessId();
        return ResponseEntity.ok(productService.getProduct(id , businessId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id , @RequestBody @Valid ProductRequest request) {
        UUID businessId = getAuthenticatedBusinessId();
        return ResponseEntity.ok(productService.updateProduct(id , request , businessId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        UUID businessId = getAuthenticatedBusinessId();
        productService.deleteProduct(id , businessId);
        return ResponseEntity.noContent().build();
    }

    private UUID getAuthenticatedBusinessId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()
        );
    }
}
