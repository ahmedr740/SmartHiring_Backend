package com.smarthiring.controller;

import com.smarthiring.model.User;
import com.smarthiring.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.register(user);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication) {
        return userService.getByEmail(authentication.getName());
    }

    @PutMapping("/me")
    public User updateCurrentUser(@RequestBody User user, Authentication authentication) {
        return userService.updateCurrentUser(authentication.getName(), user);
    }

    @PostMapping(value = "/me/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public User uploadCurrentWorkerCv(@RequestPart("file") MultipartFile file, Authentication authentication) {
        return userService.uploadCurrentWorkerCv(authentication.getName(), file);
    }
}
