package com.smarthiring;

import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminDataInitializer {
    // This was mainly added so the team always had an admin account available
    // during demos without needing to seed data manually every time.

    @Bean
    CommandLineRunner ensureDefaultAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.seed.enabled:false}") boolean seedEnabled,
            @Value("${app.admin.seed.email:}") String adminEmail,
            @Value("${app.admin.seed.password:}") String adminPassword,
            @Value("${app.admin.seed.name:Platform Admin}") String adminName
    ) {
        return args -> {
            if (!seedEnabled) {
                return;
            }

            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.length() < 8) {
                throw new IllegalStateException("Admin seed is enabled, but ADMIN_SEED_EMAIL and ADMIN_SEED_PASSWORD are not valid");
            }

            if (userRepository.countByRoleIgnoreCase("ADMIN") > 0) {
                return;
            }

            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            admin.setStatus("ACTIVE");
            admin.setRating(0d);
            admin.setRatingCount(0);
            admin.setCompletedShiftsCount(0);
            userRepository.save(admin);
        };
    }
}
