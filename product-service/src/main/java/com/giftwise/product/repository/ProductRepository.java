package com.giftwise.product.repository;

import com.giftwise.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Fetch every product for a business, active and inactive — feeds the dashboard's
     * full catalog view.
     *
     * @param businessId : id of the owning business
     * @return all products belonging to {@code businessId}, regardless of active status
     */
    List<Product> findAllByBusinessId(UUID businessId);

    /**
     * Fetch only active products for a business — feeds the gRPC search path consumed
     * by recommendation-service, which should never surface deactivated products.
     *
     * @param businessId : id of the owning business
     * @return active products belonging to {@code businessId}
     */
    List<Product> findAllByBusinessIdAndIsActiveTrue(UUID businessId);

    /**
     * Fetch a single product, scoped to its owning business in the same query — combines
     * the existence check and the ownership check so a business can never reach another
     * business's product, even by id.
     *
     * @param id         : id of the product to fetch
     * @param businessId : id of the business that must own this product
     * @return the matching product, or empty if it doesn't exist or belongs to another business
     */
    Optional<Product> findByIdAndBusinessId(UUID id , UUID businessId);

    /**
     * Semantic similarity search with optional scalar filters: rank a business's active
     * products by cosine distance between their stored embedding and a query embedding,
     * narrowed by category, occasion, age group, and/or price range.
     * <p>
     * Native SQL is required because {@code <->} (cosine distance) and the {@code vector}
     * type are pgvector-specific — JPQL has no concept of either. The query embedding is
     * passed as a {@code String} and cast to {@code vector} inside the SQL itself, since
     * Spring Data can't bind a {@code PGvector}/{@code float[]} as a native query parameter.
     * Each filter uses the {@code (:param IS NULL OR column = :param)} pattern so a caller
     * can pass {@code null} to skip that filter entirely — with every filter {@code null},
     * this query behaves identically to an unfiltered semantic search. Scalar filters run
     * first to shrink the candidate set before the (more expensive) vector ordering kicks in.
     *
     * @param businessId : id of the business whose catalog to search
     * @param embedding  : the query embedding, formatted as pgvector text e.g. {@code "[0.1, 0.2, ...]"}
     * @param category   : exact category to filter on, or {@code null} to skip this filter
     * @param occasion   : exact occasion to filter on, or {@code null} to skip this filter
     * @param ageGroup   : exact age group to filter on, or {@code null} to skip this filter
     * @param minPrice   : minimum price (inclusive), or {@code null} to skip this filter
     * @param maxPrice   : maximum price (inclusive), or {@code null} to skip this filter
     * @param limit      : maximum number of results to return (top-K nearest neighbors)
     * @return the business's active products matching all provided filters, closest in meaning
     * to {@code embedding} first
     */
    @Query(value = """
    SELECT * FROM products
    WHERE business_id = :businessId
    AND is_active = true
    AND (:category IS NULL OR category = :category)
    AND (:occasion IS NULL OR occasion = :occasion)
    AND (:ageGroup IS NULL OR age_group = :ageGroup)
    AND (:minPrice IS NULL OR price >= :minPrice)
    AND (:maxPrice IS NULL OR price <= :maxPrice)
    ORDER BY embedding <-> CAST(:embedding AS vector)
    LIMIT :limit
    """, nativeQuery = true)
    List<Product> findSimilarProductsWithFilters(
            @Param("businessId") UUID businessId,
            @Param("embedding") String embedding,
            @Param("category") String category,
            @Param("occasion") String occasion,
            @Param("ageGroup") String ageGroup,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("limit") int limit
    );







    /**
     * Insert a new product row with an explicit embedding value via native SQL.
     * <p>
     * Required because the {@code embedding} column is {@code NOT NULL vector(1536)} and plain
     * {@code save()} cannot supply a placeholder vector for a field Hibernate doesn't manage as
     * a first-class type. {@code ProductService.insertProduct} passes a zero-vector here; the
     * real embedding is written moments later by {@link #updateEmbedding}.
     * {@code clearAutomatically = true} evicts the persistence context so a subsequent
     * {@code findById} reads the row this statement just wrote, not a stale cached entity.
     *
     * @param id          : pre-generated id for the new product (generated in the service, not the DB)
     * @param businessId  : id of the business that owns this product
     * @param name        : product name
     * @param description : product description
     * @param price       : product price
     * @param imageUrl    : URL of the product image
     * @param category    : product category, used as a scalar filter in search
     * @param occasion    : occasion this product suits, used as a scalar filter in search
     * @param ageGroup    : target age group, used as a scalar filter in search
     * @param embedding   : placeholder embedding formatted as pgvector text, cast to {@code vector} in SQL
     * @param isActive    : initial active status (always {@code true} for a newly created product)
     */
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

    /**
     * Soft-delete: flips {@code is_active} to false rather than removing the row, scoped
     * to the owning business so a business can only deactivate its own products.
     *
     * @param id         : id of the product to deactivate
     * @param businessId : id of the business that must own this product
     * @return number of rows updated — 0 means no matching product for this business
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE products SET is_active = false, updated_at = NOW() WHERE id = :id AND business_id = :businessId", nativeQuery = true)
    int softDelete(@Param("id") UUID id, @Param("businessId") UUID businessId);

    /**
     * Update a product's editable fields via native SQL, scoped to the owning business.
     * <p>
     * Native SQL (rather than loading the entity and calling {@code save()}) keeps this
     * update from touching the {@code embedding} column — that column is regenerated
     * separately by {@link #updateEmbedding} once the new name/description is known.
     *
     * @param id          : id of the product to update
     * @param businessId  : id of the business that must own this product
     * @param name        : new product name
     * @param description : new product description
     * @param price       : new product price
     * @param imageUrl    : new product image URL
     * @param category    : new product category
     * @param occasion    : new occasion value
     * @param ageGroup    : new target age group
     * @return number of rows updated — 0 means no matching product for this business
     */
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

    /**
     * Overwrite a product's stored embedding — the final step of {@link
     * com.giftwise.product.service.EmbeddingService#generateAndSave}, run after a real
     * embedding has been generated for the placeholder/stale vector written at insert/update time.
     * <p>
     * Native SQL with {@code CAST(:embedding AS vector)} for the same reason as
     * {@link #insertWithEmbedding}: Spring Data has no binding for the pgvector type, so the
     * value travels as text and is cast inside the SQL. {@code clearAutomatically = true}
     * ensures the next read picks up the new embedding rather than a cached stale entity.
     *
     * @param id        : id of the product whose embedding to overwrite
     * @param embedding : the freshly generated embedding, formatted as pgvector text
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE products SET embedding = CAST(:embedding AS vector), updated_at = NOW() WHERE id = :id",
            nativeQuery = true)
    void updateEmbedding(@Param("id") UUID id, @Param("embedding") String embedding);
}
