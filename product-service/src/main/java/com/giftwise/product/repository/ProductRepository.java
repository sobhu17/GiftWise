package com.giftwise.product.repository;

import com.giftwise.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findAllByBusinessId(UUID businessId);
    List<Product> findAllByBusinessIdAndIsActiveTrue(UUID businessId);
    Optional<Product> findByIdAndBusinessId(UUID id , UUID businessId);

    @Query(value = """
    SELECT * FROM products
    WHERE business_id = :businessId
    AND is_active = true
    ORDER BY embedding <-> CAST(:embedding AS vector)
    LIMIT :limit
    """, nativeQuery = true)
    List<Product> findSimilarProducts(
            @Param("businessId") UUID businessId,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = """
    INSERT INTO products 
    (id, business_id, name, description, price, image_url, category, 
     occasion, age_group, embedding, is_active, created_at, updated_at)
    VALUES (:id, :businessId, :name, :description, :price, :imageUrl, 
            :category, :occasion, :ageGroup, 
            CAST(:embedding AS vector), :isActive, NOW(), NOW())
    """, nativeQuery = true)
    void insertWithEmbedding(
            @Param("id") UUID id,
            @Param("businessId") UUID businessId,
            @Param("name") String name,
            @Param("description") String description,
            @Param("price") java.math.BigDecimal price,
            @Param("imageUrl") String imageUrl,
            @Param("category") String category,
            @Param("occasion") String occasion,
            @Param("ageGroup") String ageGroup,
            @Param("embedding") String embedding,
            @Param("isActive") boolean isActive
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE products SET is_active = false, updated_at = NOW() WHERE id = :id AND business_id = :businessId", nativeQuery = true)
    int softDelete(@Param("id") UUID id, @Param("businessId") UUID businessId);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE products SET 
    name = :name, description = :description, price = :price,
    image_url = :imageUrl, category = :category, occasion = :occasion,
    age_group = :ageGroup, updated_at = NOW()
    WHERE id = :id AND business_id = :businessId
    """, nativeQuery = true)
    int updateProduct(
            @Param("id") UUID id,
            @Param("businessId") UUID businessId,
            @Param("name") String name,
            @Param("description") String description,
            @Param("price") java.math.BigDecimal price,
            @Param("imageUrl") String imageUrl,
            @Param("category") String category,
            @Param("occasion") String occasion,
            @Param("ageGroup") String ageGroup
    );
}
