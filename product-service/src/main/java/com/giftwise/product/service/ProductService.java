package com.giftwise.product.service;

import com.giftwise.product.dto.ProductRequest;
import com.giftwise.product.dto.ProductResponse;
import com.giftwise.product.exception.ProductNotFoundException;
import com.giftwise.product.model.Product;
import com.giftwise.product.repository.ProductRepository;
import com.pgvector.PGvector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public ProductResponse createProduct(ProductRequest request, UUID businessId) {
        UUID id = UUID.randomUUID();
        String zeroVector = "[" + "0.0,".repeat(1535) + "0.0]";

        productRepository.insertWithEmbedding(
                id, businessId,
                request.getName(), request.getDescription(), request.getPrice(),
                request.getImageUrl(), request.getCategory(), request.getOccasion(),
                request.getAgeGroup(), zeroVector, true
        );

        return ProductResponse.from(
                productRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Product not found after insert"))
        );
    }

    public ProductResponse getProduct(UUID id , UUID businessId) {
        Product product = productRepository.findByIdAndBusinessId(id , businessId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request, UUID businessId) {
        // Ownership check — throws if not found or wrong business
        productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        productRepository.updateProduct(
                id, businessId,
                request.getName(), request.getDescription(), request.getPrice(),
                request.getImageUrl(), request.getCategory(), request.getOccasion(),
                request.getAgeGroup()
        );

        entityManager.flush();
        entityManager.clear();  // clears first-level cache — next findById hits DB

        return ProductResponse.from(
                productRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Product not found after update"))
        );
    }

    @Transactional
    public void deleteProduct(UUID id, UUID businessId) {
        productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        productRepository.softDelete(id, businessId);
    }

    public List<ProductResponse> listProducts(UUID businessId) {
        return productRepository.findAllByBusinessId(businessId)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    private void setProductAttributes(Product product , ProductRequest request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());
        product.setOccasion(request.getOccasion());
        product.setAgeGroup(request.getAgeGroup());
    }

}
