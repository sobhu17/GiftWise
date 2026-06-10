package com.giftwise.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Translate a {@link BusinessAlreadyExistsException} into a 409 response with a JSON
     * error body.
     * <p>
     * Centralizing this here keeps {@code BusinessService} free of any HTTP concerns — it just
     * throws a plain unchecked exception, and this {@code @ControllerAdvice} maps it to the
     * right status code for every controller in the service.
     *
     * @param ex : the exception thrown when registering with an email that's already in use
     * @return 409 Conflict with body {@code {"error": "<message>"}}
     */
    @ExceptionHandler(BusinessAlreadyExistsException.class)
    public ResponseEntity<Map<String , String>> handleBusinessAlreadyExistsException(BusinessAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Translate an {@link InvalidCredentialsException} into a 401 response with a JSON
     * error body.
     *
     * @param ex : the exception thrown when a login password doesn't match
     * @return 401 Unauthorized with body {@code {"error": "<message>"}}
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Translate Jakarta validation failures on {@code @Valid @RequestBody} arguments into a
     * 400 response listing each invalid field and its message.
     *
     * @param ex : the exception thrown when request body validation fails
     * @return 400 Bad Request with body {@code {"<field>": "<message>", ...}} for each invalid field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    /**
     * Catch-all fallback for any exception not handled by a more specific handler above.
     * <p>
     * Returns a generic message rather than {@code ex.getMessage()} — an unexpected exception
     * (e.g. a database error) could otherwise leak internal details to the caller.
     *
     * @param ex : the unhandled exception
     * @return 500 Internal Server Error with a generic error body
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred"));
    }
}
