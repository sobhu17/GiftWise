package com.giftwise.product.service;

import com.giftwise.product.dto.ProductRequest;
import com.giftwise.product.dto.ProductResponse;
import com.giftwise.product.exception.ProductNotFoundException;
import com.giftwise.product.model.Product;
import com.giftwise.product.repository.ProductRepository;
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
    private final EmbeddingService embeddingService;

    /**
     * Create a new product for the given business and synchronously generate its embedding.
     * <p>
     * The insert runs in its own transaction (see {@link #insertProduct}) and commits with a
     * placeholder zero-vector embedding. The embedding generation call happens outside that
     * transaction — if the OpenAI call is slow, it does not hold a DB connection/lock open.
     *
     * @param request    : validated product fields submitted by the business
     * @param businessId : id of the authenticated business, extracted from the JWT
     * @return the freshly created product, including its real embedding, mapped to a response DTO
     */
    public ProductResponse createProduct(ProductRequest request, UUID businessId) {
        UUID id = insertProduct(request, businessId);  // @Transactional
        embeddingService.generateAndSave(id);          // outside transaction
        return ProductResponse.from(
                productRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Product not found after insert"))
        );
    }

    /**
     * Insert the product row with a placeholder zero-vector embedding.
     * <p>
     * A real {@code vector(1536)} value is required at insert time (NOT NULL column), but the
     * actual embedding isn't known yet — it is filled in by {@link EmbeddingService} right after
     * this transaction commits. A vector of all zeros is the simplest valid placeholder.
     *
     * @param request    : validated product fields submitted by the business
     * @param businessId : id of the authenticated business that owns this product
     * @return the generated id of the newly inserted product row
     */
    @Transactional
    protected UUID insertProduct(ProductRequest request, UUID businessId) {
        UUID id = UUID.randomUUID();
        String zeroVector = "[" + "0.0,".repeat(1535) + "0.0]";
        productRepository.insertWithEmbedding(
                id, businessId,
                request.getName(), request.getDescription(), request.getPrice(),
                request.getImageUrl(), request.getCategory(), request.getOccasion(),
                request.getAgeGroup(), zeroVector, true
        );
        return id;
    }

    /**
     * Fetch a single product, scoped to the requesting business.
     * <p>
     * {@code findByIdAndBusinessId} does both the existence check and the ownership
     * check in one query — a business can never see another business's product, even by guessing
     * its id.
     *
     * @param id         : id of the product to fetch
     * @param businessId : id of the authenticated business, used as an ownership filter
     * @return the matching product mapped to a response DTO
     * @throws ProductNotFoundException if no product with this id exists for this business
     */
    public ProductResponse getProduct(UUID id , UUID businessId) {
        Product product = productRepository.findByIdAndBusinessId(id , businessId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        return ProductResponse.from(product);
    }

    /**
     * Update an existing product's fields and regenerate its embedding to match the new
     * name/description.
     * <p>
     * Same split as {@link #createProduct}: the field update commits in its own transaction
     * (see {@link #executeUpdate}), then the embedding is regenerated outside that transaction
     * since the text driving the embedding (name + description) may have changed.
     *
     * @param id         : id of the product to update
     * @param request    : validated replacement fields submitted by the business
     * @param businessId : id of the authenticated business, used as an ownership filter
     * @return the updated product, including its regenerated embedding, mapped to a response DTO
     * @throws ProductNotFoundException if no product with this id exists for this business
     */
    public ProductResponse updateProduct(UUID id, ProductRequest request, UUID businessId) {
        executeUpdate(id, request, businessId);        // @Transactional
        embeddingService.generateAndSave(id);          // outside transaction
        return ProductResponse.from(
                productRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Product not found after update"))
        );
    }

    /**
     * Apply the field updates via a native {@code UPDATE} query, then clear the persistence
     * context so the next read returns fresh data instead of a stale cached entity.
     * <p>
     * {@code entityManager.clear()} is required here because native {@code @Modifying} queries
     * bypass Hibernate's first-level cache — without clearing it, {@code findById} after this
     * method would return the old in-memory entity rather than the row we just updated.
     *
     * @param id         : id of the product to update
     * @param request    : validated replacement fields submitted by the business
     * @param businessId : id of the authenticated business, used as an ownership filter
     * @throws ProductNotFoundException if no product with this id exists for this business
     */
    @Transactional
    protected void executeUpdate(UUID id, ProductRequest request, UUID businessId) {
        productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        productRepository.updateProduct(
                id, businessId,
                request.getName(), request.getDescription(), request.getPrice(),
                request.getImageUrl(), request.getCategory(), request.getOccasion(),
                request.getAgeGroup()
        );

        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Soft-delete a product by flipping {@code is_active} to false.
     * <p>
     * Products are never hard-deleted — the row stays so the business can still see it in their
     * dashboard history, and so analytics referencing this product id remain valid.
     *
     * @param id         : id of the product to deactivate
     * @param businessId : id of the authenticated business, used as an ownership filter
     * @throws ProductNotFoundException if no product with this id exists for this business
     */
    @Transactional
    public void deleteProduct(UUID id, UUID businessId) {
        productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        productRepository.softDelete(id, businessId);
    }

    /**
     * List every product owned by the business — active and inactive — for the catalog
     * management dashboard.
     * <p>
     * Deliberately uses {@code findAllByBusinessId} (not the *AndIsActiveTrue variant): the
     * dashboard needs to show the full history including deactivated products, whereas the
     * gRPC search path used by recommendation-service only ever returns active ones.
     *
     * @param businessId : id of the authenticated business
     * @return every product belonging to this business, mapped to response DTOs
     */
    public List<ProductResponse> listProducts(UUID businessId) {
        return productRepository.findAllByBusinessId(businessId)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
}
