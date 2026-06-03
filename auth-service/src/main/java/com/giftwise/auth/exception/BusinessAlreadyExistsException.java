package com.giftwise.auth.exception;

public class BusinessAlreadyExistsException extends RuntimeException{
    public BusinessAlreadyExistsException(String message) {
        super(message);
    }
}
