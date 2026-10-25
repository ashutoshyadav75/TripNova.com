package com.tripnova.tripnova.Service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tripnova.tripnova.Entity.User;
import com.tripnova.tripnova.Repository.UserRepository;
import com.tripnova.tripnova.dto.RegisterForm;

import jakarta.transaction.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterForm form) {

        String email = form.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }

        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(form.getPassword())
        );

        userRepository.save(user);
    }
}
