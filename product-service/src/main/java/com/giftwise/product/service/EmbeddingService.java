package com.giftwise.product.service;

import com.giftwise.product.exception.ProductNotFoundException;
import com.giftwise.product.model.Product;
import com.giftwise.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
    private final ProductRepository productRepository;
    private final WebClient webClient;

    @Value("${giftwise.openai.api-key}")
    private String apiKey;

    @Value("${giftwise.openai.embedding-model}")
    private String embeddingModel;

    @Value("${giftwise.openai.base-url}")
    private String baseUrl;


    /**
     * Generate a fresh embedding for a product from its name + description and persist it.
     * <p>
     * Runs synchronously, called right after the product row is committed (see
     * {@code ProductService.createProduct} / {@code updateProduct}). Both name and description
     * are concatenated as input — together they give the embedding model the richest possible
     * signal about the product's meaning, which is what makes semantic search return relevant
     * results later.
     *
     * @param productId : id of the product to (re)generate an embedding for
     * @throws ProductNotFoundException if no product exists with this id
     */
    public void generateAndSave(UUID productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        String inputText = product.getName() + " — " + product.getDescription();
        float[] embedding = callEmbeddingApi(inputText);
        String vectorString = toVectorString(embedding);
        productRepository.updateEmbedding(productId, vectorString);
    }

    /**
     * Call the OpenAI embeddings endpoint and extract the resulting vector.
     *
     * @param inputText : the text to embed (product name + description, or a search query)
     * @return a 1536-dimension float array — the embedding model's output for {@code inputText}
     */
    public float[] callEmbeddingApi(String inputText) {
        EmbeddingRequest requestBody = new EmbeddingRequest(embeddingModel, inputText);

        EmbeddingResponse response = webClient.post()
                .uri(baseUrl + "/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new RuntimeException("Empty response from OpenAI embedding API");
        }

        return response.data().get(0).embedding();
    }

    /**
     * Format a float array as the bracketed, comma-separated string pgvector expects
     * over the wire, e.g. {@code "[0.5, -0.3, 0.8]"}.
     * <p>
     * The repository casts this string to {@code vector} inside the SQL itself
     * ({@code CAST(:embedding AS vector)}) — JDBC has no native pgvector binding, so the
     * value has to travel as text.
     *
     * @param embedding : the raw embedding returned by the embedding API
     * @return the embedding formatted as pgvector's textual vector representation
     */
    public String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private record EmbeddingRequest(String model, String input) {}

    private record EmbeddingResponse(List<EmbeddingData> data) {}

    private record EmbeddingData(float[] embedding, int index) {}

}
