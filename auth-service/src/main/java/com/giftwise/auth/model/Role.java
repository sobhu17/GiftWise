package com.giftwise.auth.model;

import com.giftwise.auth.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {
    @Column(name = "name", nullable = false, unique = true)
    private String name;
}
