package com.giftwise.product.grpc;

import com.giftwise.product.dto.ProductResponse;
import com.giftwise.product.service.ProductService;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * gRPC server implementation for product-service. Registered on the gRPC port
 * (9084) by grpc-spring-boot-starter — not on the HTTP port used by REST controllers.
 */
@GrpcService
@RequiredArgsConstructor
public class ProductGrpcServiceImpl extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase{
    private final ProductService productService;


    /**
     * Translates the proto request to a REST-layer DTO, delegates to ProductService,
     * then maps each entity back to a proto Product message.
     * Optional filter fields use hasXxx() — proto3 has no null, so "" != "not set";
     * hasXxx() returns false only when the caller never set the field.
     */
    @Override
    public void searchProducts(ProductSearchRequest request, StreamObserver<ProductSearchResponse> responseObserver) {
        try {
            com.giftwise.product.dto.ProductSearchRequest searchRequest =
                    new com.giftwise.product.dto.ProductSearchRequest();
            searchRequest.setQuery(request.getQuery());
            searchRequest.setCategory(request.hasCategory() ? request.getCategory() : null);
            searchRequest.setOccasion(request.hasOccasion() ? request.getOccasion() : null);
            searchRequest.setAgeGroup(request.hasAgeGroup() ? request.getAgeGroup() : null);
            searchRequest.setMinPrice(request.hasMinPrice() ? new BigDecimal(request.getMinPrice()) : null);
            searchRequest.setMaxPrice(request.hasMaxPrice() ? new BigDecimal(request.getMaxPrice()) : null);
            searchRequest.setLimit(request.getLimit() > 0 ? request.getLimit() : 10);

            UUID businessId = UUID.fromString(request.getBusinessId());

            List<ProductResponse> results = productService.searchProducts(searchRequest, businessId);
            List<Product> protoProducts = results.stream()
                    .map(this::toProtoProduct)
                    .toList();

            ProductSearchResponse response = ProductSearchResponse.newBuilder()
                    .addAllProducts(protoProducts)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Search failed: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    /**
     * ProductNotFoundException is caught separately and mapped to Status.NOT_FOUND
     * so callers can distinguish "product doesn't exist" from a server-side failure.
     */
    @Override
    public void getProduct(GetProductRequest request, StreamObserver<Product> responseObserver) {
        try {
            UUID productId = UUID.fromString(request.getId());
            UUID businessId = UUID.fromString(request.getBusinessId());

            ProductResponse dto = productService.getProduct(productId, businessId);

            responseObserver.onNext(toProtoProduct(dto));
            responseObserver.onCompleted();

        } catch (com.giftwise.product.exception.ProductNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get product: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    /**
     * Maps a REST ProductResponse to a proto Product message.
     * LocalDateTime has no timezone, so toInstant(ZoneOffset.UTC) is used —
     * this matches how the DB stores the value (no timezone column).
     */
    private com.giftwise.product.grpc.Product toProtoProduct(ProductResponse dto) {
        return com.giftwise.product.grpc.Product.newBuilder()
                .setId(dto.getId().toString())
                .setBusinessId(dto.getBusinessId().toString())
                .setName(dto.getName())
                .setDescription(dto.getDescription())
                .setPrice(dto.getPrice().toPlainString())
                .setImageUrl(dto.getImageUrl())
                .setCategory(dto.getCategory())
                .setOccasion(dto.getOccasion())
                .setAgeGroup(dto.getAgeGroup())
                .setActive(dto.isActive())
                .setCreatedAt(Timestamp.newBuilder()
                        .setSeconds(dto.getCreatedAt().toInstant(ZoneOffset.UTC).getEpochSecond())
                        .setNanos(dto.getCreatedAt().toInstant(ZoneOffset.UTC).getNano())
                        .build())
                .build();
    }
}
