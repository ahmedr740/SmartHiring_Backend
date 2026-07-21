package com.smarthiring.controller;

import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.service.MockPaymentService;
import com.smarthiring.service.ShiftService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService shiftService;
    private final MockPaymentService mockPaymentService;
    private final UserRepository userRepository;

    public ShiftController(ShiftService shiftService, MockPaymentService mockPaymentService, UserRepository userRepository) {
        this.shiftService = shiftService;
        this.mockPaymentService = mockPaymentService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Shift> getAllShifts(Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return shiftService.getVisibleShifts(currentUser);
    }

    @PostMapping
    public Shift createShift(@RequestBody Shift shift, Authentication authentication) {
        User manager = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (!"MANAGER".equalsIgnoreCase(manager.getRole())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only managers can create shifts");
        }

        if (!"ACTIVE".equalsIgnoreCase(manager.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "This manager account is not approved to post shifts");
        }

        shift.setManager(manager);
        return shiftService.createShift(shift);
    }

    @PutMapping("/{id}/status")
    public Shift updateShiftStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> request, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return shiftService.updateStatus(id, request.get("status"), currentUser);
    }

    @PutMapping("/{id}")
    public Shift updateShift(@PathVariable Long id, @RequestBody Shift shift, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return shiftService.updateShift(id, shift, currentUser);
    }

    @PutMapping("/{id}/payment")
    public Shift markPaid(@PathVariable Long id, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mockPaymentService.startAndConfirm(id, currentUser).getShift();
    }

    @DeleteMapping("/{id}")
    public void deleteShift(@PathVariable Long id, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        shiftService.deleteShift(id, currentUser);
    }
}
