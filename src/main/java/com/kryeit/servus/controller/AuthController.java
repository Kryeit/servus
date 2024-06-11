package com.kryeit.servus.controller;

import com.kryeit.servus.auth.User;
import com.kryeit.servus.auth.UserRepository;
import com.kryeit.servus.service.MojangService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MojangService mojangService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, MojangService mojangService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mojangService = mojangService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestParam String username, @RequestParam String password) {
        UUID uuid;
        try {
            uuid = mojangService.getMinecraftUUID(username);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch UUID for the provided username");
        }

        if (userRepository.existsById(uuid)) {
            return ResponseEntity.badRequest().body("UUID is already registered");
        }

        User user = new User(uuid, passwordEncoder.encode(password));
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully with username: " + username);
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestParam String username, @RequestParam String password) {
        UUID uuid;
        try {
            uuid = mojangService.getMinecraftUUID(username);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch UUID for the provided username");
        }

        User user = userRepository.findById(uuid).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid username or password");
        }

        // Spring Security handles login
        return ResponseEntity.ok("Login successful");
    }
}
