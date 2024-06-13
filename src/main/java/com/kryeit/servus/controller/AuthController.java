package com.kryeit.servus.controller;

import com.kryeit.servus.auth.User;
import com.kryeit.servus.auth.UserRepository;
import com.kryeit.servus.service.MojangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MojangService mojangService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, MojangService mojangService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mojangService = mojangService;
        this.authenticationManager = authenticationManager;
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
    public ResponseEntity<String> loginUser(@RequestParam String uuidString, @RequestParam String password) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(uuidString, password);
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return ResponseEntity.ok("Login successful");
    }
}
