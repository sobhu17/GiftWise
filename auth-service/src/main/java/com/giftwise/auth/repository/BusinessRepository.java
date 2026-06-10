package com.giftwise.auth.repository;

import com.giftwise.auth.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    /**
     * Look up a business by its login email — used for both registration's
     * duplicate-email check and login's credential lookup.
     *
     * @param email : email address to look up
     * @return the matching business, or empty if no business is registered with this email
     */
    Optional<Business> findByEmail(String email);
}
