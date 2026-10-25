package com.tripnova.tripnova.Controller;




import com.tripnova.tripnova.Entity.*;
import com.tripnova.tripnova.Repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {

        User user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Logged-in user not found"));

        model.addAttribute("user", user);

        return "profile";
    }
}
