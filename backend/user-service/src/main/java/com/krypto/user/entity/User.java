package com.krypto.user.entity;

import com.krypto.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String avatar;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.PLAYER;

    @Builder.Default
    private boolean enabled = true;
}
