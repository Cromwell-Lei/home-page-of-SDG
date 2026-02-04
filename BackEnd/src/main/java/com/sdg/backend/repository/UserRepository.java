package com.sdg.backend.repository;

import com.sdg.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndRole(String email, String role);

    Optional<User> findByUsernameAndRole(String username, String role);

    @org.springframework.data.jpa.repository.Query("SELECT u.id FROM User u ORDER BY u.id ASC")
    java.util.List<Long> findAllIds();
}
