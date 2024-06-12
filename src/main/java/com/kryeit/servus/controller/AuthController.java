package com.kryeit.servus.controller;

import com.kryeit.servus.auth.User;
import com.kryeit.servus.auth.UserRepository;
import com.kryeit.servus.service.MojangService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, MojangService mojangService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mojangService = mojangService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestParam String uuidString, @RequestParam String password) {
        UUID uuid = UUID.fromString(uuidString);
        if (userRepository.existsById(uuid)) {
            return ResponseEntity.badRequest().body("UUID is already registered");
        }
        String username = mojangService.getMinecraftUsername(uuid);
        User user = new User(uuid, passwordEncoder.encode(password));
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully with username: " + username);
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser() {
        // Spring Security handles login
        return ResponseEntity.ok("Login successful");
    }
}
