package com.tripnova.tripnova.Config;



import com.tripnova.tripnova.Entity.User;
import com.tripnova.tripnova.Repository.*;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

	






    private final UserRepository userRepository;

    public GlobalModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal()
                    .equals("anonymousUser")) {
            return null;
        }

        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .map(User::getFullName)
                .orElse(authentication.getName());
    }
}


