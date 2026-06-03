package com.giftwise.product.service;

import com.giftwise.product.dto.ProductRequest;
import com.giftwise.product.dto.ProductResponse;
import com.giftwise.product.exception.ProductNotFoundException;
import com.giftwise.product.model.Product;
import com.giftwise.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest request , UUID businessId) {
        Product product = new Product();
        setProductAttributes(product , request);
        product.setBusinessId(businessId);
        product.setActive(true);
        // Still have to add embeddings for a product -> create product
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse getProduct(UUID id , UUID businessId) {
        Product product = productRepository.findByIdAndBusinessId(id , businessId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        return ProductResponse.from(product);
    }

    public ProductResponse updateProduct(UUID id , ProductRequest request , UUID businessId) {
        Product product = productRepository.findByIdAndBusinessId(id , businessId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        setProductAttributes(product , request);
        // Still have to add embeddings for a product -> update product
        return ProductResponse.from(productRepository.save(product));
    }

    public void deleteProduct(UUID id , UUID businessId) {
        Product product = productRepository.findByIdAndBusinessId(id , businessId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
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
