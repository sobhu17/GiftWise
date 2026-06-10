package com.giftwise.auth.service;

import com.giftwise.auth.dto.AuthResponse;
import com.giftwise.auth.exception.BusinessAlreadyExistsException;
import com.giftwise.auth.exception.InvalidCredentialsException;
import com.giftwise.auth.model.Business;
import com.giftwise.auth.model.Role;
import com.giftwise.auth.repository.BusinessRepository;
import com.giftwise.auth.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BusinessService {
    private final PasswordEncoder passwordEncoder;
    private final BusinessRepository businessRepository;
    private final RoleRepository roleRepository;
    private final TokenService tokenService;

    /**
     * Register a new business account, hashing its password and assigning the default
     * {@code BUSINESS_OWNER} role.
     * <p>
     * Runs in a single transaction so the business row and its role assignment either both
     * commit or neither does — a business is never left without a role.
     *
     * @param name     : display name of the business
     * @param email    : business email, used as the unique login identifier
     * @param password : plaintext password, hashed with {@link PasswordEncoder} before storage
     * @return the newly created business along with a signed JWT
     * @throws BusinessAlreadyExistsException if a business with this email already exists
     */
    @Transactional
    public AuthResponse register(String name, String email, String password) {
        if (businessRepository.findByEmail(email).isPresent()) {
            throw new BusinessAlreadyExistsException("Business already exists with email: " + email);
        }

        Role role = roleRepository.findByNameIgnoreCase("BUSINESS_OWNER")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Business business = new Business();
        business.setName(name);
        business.setEmail(email);
        business.setPassword(passwordEncoder.encode(password));
        business.setActive(true);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        business.setRoles(roles);
        businessRepository.save(business);

        String token = tokenService.generateToken(business);
        return AuthResponse.from(business , token);
    }

    /**
     * Authenticate a business by email and password and issue a fresh JWT.
     *
     * @param email    : business email to look up
     * @param password : plaintext password, checked against the stored hash
     * @return the authenticated business along with a signed JWT
     * @throws InvalidCredentialsException if the password does not match
     */
    public AuthResponse login(String email, String password) {
        Business business = businessRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Business not found with email: " + email));


        if(!passwordEncoder.matches(password , business.getPassword())){
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = tokenService.generateToken(business);
        return AuthResponse.from(business , token);
    }
}
