package com.krypto.user.repository;

import com.krypto.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

        @Query("""
                        select u from User u
                        where u.enabled = true
                            and (
                                lower(u.username) like lower(concat('%', :query, '%'))
                                or lower(u.email) like lower(concat('%', :query, '%'))
                            )
                        """)
        Page<User> searchEnabledByUsernameOrEmail(@Param("query") String query, Pageable pageable);
}
