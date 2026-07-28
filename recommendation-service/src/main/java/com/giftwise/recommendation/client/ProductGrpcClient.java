package com.giftwise.recommendation.client;

import com.giftwise.product.grpc.Product;
import com.giftwise.product.grpc.ProductGrpcServiceGrpc;
import com.giftwise.product.grpc.ProductSearchRequest;
import com.giftwise.product.grpc.ProductSearchResponse;
import com.giftwise.recommendation.dto.ProductResult;
import com.giftwise.recommendation.dto.RecipientProfile;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
public class ProductGrpcClient {
    @GrpcClient("product-service")
    private ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productServiceStub;

    @Value("${giftwise.product.search-limit}")
    private int searchLimit;

    /**
     * Search product-service's catalog for products matching the recipient profile, over gRPC.
     * <p>
     * {@code businessId} is a separate parameter rather than a field on
     * {@code recipientProfile} — gRPC has no JWT/SecurityContext, so the caller must scope the
     * search explicitly, the same rule {@code ProductSearchRequest.business_id} itself follows.
     * Optional filters ({@code occasion}, {@code ageGroup}, min/max price) are only set on the
     * request when present on the profile, leaving them unset (not filtered) otherwise;
     * {@code category} has no equivalent field on {@link RecipientProfile} and is always left
     * unset.
     *
     * @param recipientProfile : the profile extracted so far this session, used to build the
     *                          semantic search query and scalar filters
     * @param businessId       : id of the business whose catalog to search
     * @return the matching active products, nearest first, mapped to this service's own
     * {@link ProductResult} DTOs
     */
    public List<ProductResult> searchProducts(RecipientProfile recipientProfile, UUID businessId){
        String query = buildQuery(recipientProfile);

        ProductSearchRequest.Builder requestBuilder =
                ProductSearchRequest.newBuilder()
                        .setQuery(query)
                        .setBusinessId(businessId.toString())
                        .setLimit(searchLimit);

        if (recipientProfile.getOccasion() != null)  requestBuilder.setOccasion(recipientProfile.getOccasion());
        if (recipientProfile.getAgeGroup() != null)  requestBuilder.setAgeGroup(recipientProfile.getAgeGroup());
        if (recipientProfile.getMinPrice() != null)  requestBuilder.setMinPrice(recipientProfile.getMinPrice().toPlainString());
        if (recipientProfile.getMaxPrice() != null)  requestBuilder.setMaxPrice(recipientProfile.getMaxPrice().toPlainString());

        ProductSearchResponse response = productServiceStub.searchProducts(requestBuilder.build());

        return response.getProductsList().stream()
                .map(this::toProductResult)
                .toList();
    }

    /**
     * Turn the structured {@link RecipientProfile} into a free-text sentence for
     * product-service to embed and compare against stored product descriptions.
     * <p>
     * Each piece (relationship, occasion, age group, interests) is appended only if present —
     * a conversation may not have mentioned all of them yet, and an unmentioned field should
     * not show up in the generated sentence as a literal "null".
     *
     * @param profile : the profile to describe in natural language
     * @return a natural-language description of the gift recipient
     */
    private String buildQuery(RecipientProfile profile) {
        StringBuilder query = new StringBuilder("Gift for");

        if (profile.getRelationship() != null)
            query.append(" a ").append(profile.getRelationship());

        if (profile.getOccasion() != null)
            query.append(" on their ").append(profile.getOccasion());

        if (profile.getAgeGroup() != null)
            query.append(", ").append(profile.getAgeGroup());

        if (profile.getInterests() != null && !profile.getInterests().isEmpty())
            query.append(" who enjoys ").append(String.join(", ", profile.getInterests()));

        return query.toString();
    }

    /**
     * Maps a proto {@code Product} to this service's own {@link ProductResult} DTO.
     * <p>
     * Kept as a distinct type from both the proto message and product-service's own
     * {@code ProductResponse} — services don't share Java classes across the module boundary
     * (same reasoning as the Day 4 DTOs). {@code createdAt} reverses the exact conversion
     * {@code ProductGrpcServiceImpl.toProtoProduct} performs: {@link LocalDateTime#ofEpochSecond}
     * from the proto {@code Timestamp}'s seconds/nanos, in UTC, matching how it was built.
     *
     * @param proto : the product returned by product-service over gRPC
     * @return the equivalent {@link ProductResult}
     */
    private ProductResult toProductResult(Product proto) {
        return ProductResult.builder()
                .id(UUID.fromString(proto.getId()))
                .name(proto.getName())
                .description(proto.getDescription())
                .price(new BigDecimal(proto.getPrice()))
                .imageUrl(proto.getImageUrl())
                .category(proto.getCategory())
                .occasion(proto.getOccasion())
                .ageGroup(proto.getAgeGroup())
                .businessId(UUID.fromString(proto.getBusinessId()))
                .active(proto.getActive())
                .createdAt(LocalDateTime.ofEpochSecond(
                        proto.getCreatedAt().getSeconds(),
                        proto.getCreatedAt().getNanos(),
                        ZoneOffset.UTC))
                .build();
    }

}
