package com.giftwise.product.repository;

import com.giftwise.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
