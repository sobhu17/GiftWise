package com.giftwise.product.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class ProductApplicationConfig {
    /**
     * Provide a single shared {@link WebClient} bean for {@link com.giftwise.product.service.EmbeddingService}
     * to call the OpenAI embeddings endpoint.
     * <p>
     * {@code WebClient} (not {@code RestTemplate}) is used because it's Spring's modern,
     * non-blocking HTTP client — even though we currently call {@code .block()} to keep the
     * embedding generation synchronous (see Day 6 design decision in NOTES.md), using
     * {@code WebClient} keeps the door open to a reactive/async version later without
     * swapping the client.
     *
     * @return a default-configured {@code WebClient} shared across the service
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
