package com.smarthiring.controller;

import com.smarthiring.dto.LoginResponse;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.security.CustomUserDetailsService;
import com.smarthiring.security.JwtService;
import com.smarthiring.service.UserService;
import com.smarthiring.repository.AiMatchCacheRepository;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.LikedJobRepository;
import com.smarthiring.repository.ShiftRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = "http://localhost:3000",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}
)
public class AuthController {
    // This controller ended up handling both normal auth and a couple of demo/reset flows
    // because we needed quick recovery options while wiring the frontend and backend together.

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final ApplicationRepository applicationRepository;
    private final LikedJobRepository likedJobRepository;
    private final AiMatchCacheRepository aiMatchCacheRepository;
    private final ShiftRepository shiftRepository;
    private final boolean demoToolsEnabled;
    private final String seedAdminEmail;
    private final String seedAdminPassword;

    public AuthController(
            UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService,
            ApplicationRepository applicationRepository,
            LikedJobRepository likedJobRepository,
            AiMatchCacheRepository aiMatchCacheRepository,
            ShiftRepository shiftRepository,
            @Value("${app.demo-tools-enabled:false}") boolean demoToolsEnabled,
            @Value("${app.admin.seed.email:}") String seedAdminEmail,
            @Value("${app.admin.seed.password:}") String seedAdminPassword
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.applicationRepository = applicationRepository;
        this.likedJobRepository = likedJobRepository;
        this.aiMatchCacheRepository = aiMatchCacheRepository;
        this.shiftRepository = shiftRepository;
        this.demoToolsEnabled = demoToolsEnabled;
        this.seedAdminEmail = seedAdminEmail;
        this.seedAdminPassword = seedAdminPassword;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody User loginUser) {
        String normalizedEmail = normalizeEmail(loginUser.getEmail());
        String rawPassword = loginUser.getPassword();

        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            if (matchesPassword(rawPassword, user)) {
                String normalizedStatus = user.getStatus() == null ? "ACTIVE" : user.getStatus().trim().toUpperCase();
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());

                if ("PENDING".equals(normalizedStatus)) {
                    throw new ResponseStatusException(UNAUTHORIZED, "Your manager account is waiting for admin approval.");
                }

                if ("REJECTED".equals(normalizedStatus)) {
                    throw new ResponseStatusException(UNAUTHORIZED, "Your manager account request was not approved.");
                }

                if ("SUSPENDED".equals(normalizedStatus)) {
                    throw new ResponseStatusException(UNAUTHORIZED, "Your account has been suspended. Please contact support.");
                }

                return new LoginResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        normalizedStatus,
                        user.getRestaurantName(),
                        user.getPhone(),
                        user.getLocation(),
                        jwtService.generateToken(userDetails)
                );

            } else {
                throw new ResponseStatusException(UNAUTHORIZED, "Incorrect password");
            }

        } else {
            throw new ResponseStatusException(UNAUTHORIZED, "User not found");
        }
    }

    @PostMapping("/register")
    public User register(@RequestBody User newUser) {
        if (newUser.getPassword() == null || newUser.getPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Password is required");
        }

        return userService.register(newUser);
    }

    @PostMapping("/bootstrap-admin")
    public LoginResponse bootstrapAdmin() {
        requireDemoTools();
        requireSeedCredentials();

        User admin = userRepository.findByEmailIgnoreCase(seedAdminEmail)
                .orElseGet(User::new);

        admin.setName("Platform Admin");
        admin.setEmail(seedAdminEmail.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(seedAdminPassword));
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setRating(admin.getRating() == null ? 0d : admin.getRating());
        admin.setRatingCount(admin.getRatingCount() == null ? 0 : admin.getRatingCount());
        admin.setCompletedShiftsCount(admin.getCompletedShiftsCount() == null ? 0 : admin.getCompletedShiftsCount());

        User savedAdmin = userRepository.save(admin);

        return new LoginResponse(
                savedAdmin.getId(),
                savedAdmin.getName(),
                savedAdmin.getEmail(),
                savedAdmin.getRole(),
                savedAdmin.getStatus(),
                savedAdmin.getRestaurantName(),
                savedAdmin.getPhone(),
                savedAdmin.getLocation(),
                jwtService.generateToken(customUserDetailsService.loadUserByUsername(savedAdmin.getEmail()))
        );
    }

    @PostMapping("/reset-demo-data")
    @Transactional
    public LoginResponse resetDemoData() {
        requireDemoTools();
        requireSeedCredentials();

        applicationRepository.deleteAll();
        likedJobRepository.deleteAll();
        aiMatchCacheRepository.deleteAll();
        shiftRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setName("Platform Admin");
        admin.setEmail(seedAdminEmail.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(seedAdminPassword));
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setRating(0d);
        admin.setRatingCount(0);
        admin.setCompletedShiftsCount(0);

        User savedAdmin = userRepository.save(admin);

        return new LoginResponse(
                savedAdmin.getId(),
                savedAdmin.getName(),
                savedAdmin.getEmail(),
                savedAdmin.getRole(),
                savedAdmin.getStatus(),
                savedAdmin.getRestaurantName(),
                savedAdmin.getPhone(),
                savedAdmin.getLocation(),
                jwtService.generateToken(customUserDetailsService.loadUserByUsername(savedAdmin.getEmail()))
        );
    }

    private boolean matchesPassword(String rawPassword, User user) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }

        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return true;
        }

        if (rawPassword.equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email is required");
        }

        return email.trim().toLowerCase();
    }

    private void requireDemoTools() {
        if (!demoToolsEnabled) {
            throw new ResponseStatusException(BAD_REQUEST, "Demo maintenance tools are disabled");
        }
    }

    private void requireSeedCredentials() {
        if (seedAdminEmail == null || seedAdminEmail.isBlank() || seedAdminPassword == null || seedAdminPassword.length() < 8) {
            throw new ResponseStatusException(BAD_REQUEST, "Admin seed credentials are not configured");
        }
    }
}
