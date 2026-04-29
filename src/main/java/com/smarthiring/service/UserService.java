package com.smarthiring.service;

import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;


@Service
public class UserService {
    // Registration/profile rules changed a couple of times while the team was
    // testing worker and manager flows, so most checks stayed grouped here.

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(User user) {
        user.setEmail(normalizeEmail(user.getEmail()));
        String normalizedRole = normalizeRole(user.getRole());
        user.setRole(normalizedRole);
        initializeTrackingFields(user);

        if ("MANAGER".equals(normalizedRole)) {
            validateManagerDetails(user);
            user.setStatus("PENDING");
        } else {
            user.setStatus("ACTIVE");
            user.setRestaurantName(null);
            user.setPhone(null);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "User not found"));
    }

    public User updateCurrentUser(String email, User request) {
        User user = getByEmail(email);

        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }

        if ("WORKER".equalsIgnoreCase(user.getRole())) {
            if (request.getSkills() != null) {
                user.setSkills(cleanOptional(request.getSkills()));
            }
            if (request.getLocation() != null) {
                user.setLocation(cleanOptional(request.getLocation()));
            }
            if (request.getAvailability() != null) {
                user.setAvailability(cleanOptional(request.getAvailability()));
            }
        } else if ("MANAGER".equalsIgnoreCase(user.getRole())) {
            if (request.getRestaurantName() != null) {
                user.setRestaurantName(cleanOptional(request.getRestaurantName()));
            }
            if (request.getPhone() != null) {
                user.setPhone(cleanOptional(request.getPhone()));
            }
            if (request.getLocation() != null) {
                user.setLocation(cleanOptional(request.getLocation()));
            }

            validateManagerDetails(user);
        } else {
            throw new ResponseStatusException(FORBIDDEN, "This account cannot update profile details here");
        }

        return userRepository.save(user);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Role is required");
        }

        String normalizedRole = role.trim().toUpperCase();
        if (!List.of("MANAGER", "WORKER").contains(normalizedRole)) {
            throw new ResponseStatusException(BAD_REQUEST, "Only worker and manager self-registration is allowed");
        }

        return normalizedRole;
    }

    private void initializeTrackingFields(User user) {
        user.setRating(user.getRating() == null ? 0d : user.getRating());
        user.setRatingCount(user.getRatingCount() == null ? 0 : user.getRatingCount());
        user.setCompletedShiftsCount(user.getCompletedShiftsCount() == null ? 0 : user.getCompletedShiftsCount());
    }

    private void validateManagerDetails(User user) {
        if (isBlank(user.getRestaurantName()) || isBlank(user.getPhone()) || isBlank(user.getLocation())) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Restaurant name, phone, and location are required for manager accounts"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String cleanOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email is required");
        }

        return email.trim().toLowerCase();
    }
}
