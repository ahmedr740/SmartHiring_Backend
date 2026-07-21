package com.smarthiring.controller;

import com.smarthiring.dto.MockPaymentRequest;
import com.smarthiring.model.MockPayment;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.service.MockPaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class MockPaymentController {

    private final MockPaymentService mockPaymentService;
    private final UserRepository userRepository;

    public MockPaymentController(MockPaymentService mockPaymentService, UserRepository userRepository) {
        this.mockPaymentService = mockPaymentService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<MockPayment> getPayments(Authentication authentication) {
        return mockPaymentService.getPayments(currentUser(authentication));
    }

    @PostMapping("/shifts/{shiftId}/start")
    public MockPayment startPayment(@PathVariable Long shiftId, Authentication authentication) {
        return mockPaymentService.startPayment(shiftId, currentUser(authentication));
    }

    @PostMapping("/{paymentId}/confirm")
    public MockPayment confirmPayment(
            @PathVariable Long paymentId,
            @RequestBody(required = false) MockPaymentRequest request,
            Authentication authentication
    ) {
        return mockPaymentService.confirmPayment(paymentId, request, currentUser(authentication));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
