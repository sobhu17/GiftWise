package com.giftwise.auth.repository;

import com.giftwise.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Look up a role by name, case-insensitively — used at registration time to
     * assign the default {@code BUSINESS_OWNER} role.
     *
     * @param name : role name to look up, e.g. {@code "BUSINESS_OWNER"}
     * @return the matching role, or empty if no role with this name exists
     */
    Optional<Role> findByNameIgnoreCase(String name);
}
