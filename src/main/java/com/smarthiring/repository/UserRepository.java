package com.smarthiring.repository;

import com.smarthiring.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    List<User> findAllByRoleIgnoreCase(String role);
    List<User> findAllByRoleIgnoreCaseAndStatusIgnoreCase(String role, String status);
    long countByRoleIgnoreCase(String role);
    long countByRoleIgnoreCaseAndStatusIgnoreCase(String role, String status);
}
