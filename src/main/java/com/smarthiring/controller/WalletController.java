package com.smarthiring.controller;

import com.smarthiring.dto.WalletResponse;
import com.smarthiring.dto.WalletWithdrawalRequest;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.service.WalletService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    public WalletController(WalletService walletService, UserRepository userRepository) {
        this.walletService = walletService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public WalletResponse getWallet(Authentication authentication) {
        return walletService.getWallet(currentUser(authentication));
    }

    @PostMapping("/withdrawals")
    public WalletResponse withdraw(
            @RequestBody(required = false) WalletWithdrawalRequest request,
            Authentication authentication
    ) {
        return walletService.withdraw(currentUser(authentication), request);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
