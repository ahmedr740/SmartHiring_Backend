package com.smarthiring;

import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminDataInitializer {
    // This was mainly added so the team always had an admin account available
    // during demos without needing to seed data manually every time.

    @Bean
    CommandLineRunner ensureDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.countByRoleIgnoreCase("ADMIN") > 0) {
                return;
            }

            User admin = new User();
            admin.setName("Platform Admin");
            admin.setEmail("admin@smarthiring.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setStatus("ACTIVE");
            admin.setRating(0d);
            userRepository.save(admin);
        };
    }
}
