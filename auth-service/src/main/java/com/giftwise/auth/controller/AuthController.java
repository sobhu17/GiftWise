package com.giftwise.auth.controller;

import com.giftwise.auth.dto.AuthResponse;
import com.giftwise.auth.dto.LoginRequest;
import com.giftwise.auth.dto.RegisterRequest;
import com.giftwise.auth.exception.BusinessAlreadyExistsException;
import com.giftwise.auth.exception.InvalidCredentialsException;
import com.giftwise.auth.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final BusinessService businessService;

    /**
     * Register a new business account.
     *
     * @param request : validated name, email, and password from the request body
     * @return 200 OK with the new business and a signed JWT
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) throws BusinessAlreadyExistsException {
        AuthResponse authResponse = businessService.register(request.getName() , request.getEmail() , request.getPassword());

        return ResponseEntity.ok(authResponse);
    }

    /**
     * Authenticate an existing business and issue a fresh JWT.
     *
     * @param request : email and password from the request body
     * @return 200 OK with the business and a signed JWT
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) throws InvalidCredentialsException {
        AuthResponse authResponse = businessService.login(request.getEmail() , request.getPassword());

        return ResponseEntity.ok(authResponse);
    }

}
