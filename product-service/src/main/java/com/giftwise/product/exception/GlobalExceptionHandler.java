package com.giftwise.product.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Translate a {@link ProductNotFoundException} into a 404 response with a JSON error body.
     * <p>
     * Centralizing this here keeps {@code ProductService} free of any HTTP concerns — it just
     * throws a plain unchecked exception, and this {@code @ControllerAdvice} maps it to the
     * right status code for every controller in the service.
     *
     * @param ex : the exception thrown when a product lookup finds no matching row
     * @return 404 Not Found with body {@code {"error": "<message>"}}
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String , String>> handleProductNotFoundException(ProductNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
