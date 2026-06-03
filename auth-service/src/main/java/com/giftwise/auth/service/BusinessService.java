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
    private final JwtService jwtService;

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

        String token = jwtService.generateToken(business);
        return AuthResponse.from(business , token);
    }

    public AuthResponse login(String email, String password) {
        Business business = businessRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Business not found with email: " + email));


        if(!passwordEncoder.matches(password , business.getPassword())){
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = jwtService.generateToken(business);
        return AuthResponse.from(business , token);
    }
}
