package com.kryeit.servus.service;

import com.kryeit.servus.auth.User;
import com.kryeit.servus.auth.UserRepository;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.security.core.userdetails.User.withUsername;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MyUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String uuidString) throws UsernameNotFoundException {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Invalid UUID string: " + uuidString, e);
        }

        Optional<User> userOptional = userRepository.findById(uuid);
        if (!userOptional.isPresent()) {
            throw new UsernameNotFoundException("User not found");
        }
        User user = userOptional.get();
        UserBuilder builder = withUsername(uuidString);
        builder.password(user.getPassword());
        builder.roles("USER");
        return builder.build();
    }
}
