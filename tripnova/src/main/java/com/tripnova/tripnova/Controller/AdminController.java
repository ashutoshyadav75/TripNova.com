package com.tripnova.tripnova.Controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.tripnova.tripnova.Entity.User;
import com.tripnova.tripnova.Repository.TripRepository;
import com.tripnova.tripnova.Repository.UserRepository;
import com.tripnova.tripnova.Entity.Trip;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

	private final UserRepository userRepository;
    private final TripRepository tripRepository;

    public AdminController(
            UserRepository userRepository,
            TripRepository tripRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {

        List<User> users = userRepository.findAll();

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalTrips", tripRepository.count());
        model.addAttribute("users", users);

        return "admin/dashboard";
    }
    

@GetMapping("/admin/users/{id}/trips")
public String viewUserTrips(
        @PathVariable Long id,
        Model model) {

    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));

    List<Trip> trips =
            tripRepository.findAllByUser_EmailIgnoreCaseOrderByIdDesc(
                    user.getEmail());

    model.addAttribute("selectedUser", user);
    model.addAttribute("userTrips", trips);

    return "admin/user-trips";
}

@GetMapping("/admin/users")
public String manageUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        Model model) {

    List<User> users = userRepository.findAll();

    if (keyword != null && !keyword.trim().isEmpty()) {
        String search = keyword.trim().toLowerCase();

        users = users.stream()
                .filter(user ->
                        (user.getFullName() != null &&
                         user.getFullName().toLowerCase().contains(search))
                        ||
                        (user.getEmail() != null &&
                         user.getEmail().toLowerCase().contains(search)))
                .collect(Collectors.toList());
    }

    if (role != null && !role.isBlank()) {
        users = users.stream()
                .filter(user -> role.equalsIgnoreCase(user.getRole()))
                .collect(Collectors.toList());
    }

    model.addAttribute("users", users);
    model.addAttribute("keyword", keyword == null ? "" : keyword);
    model.addAttribute("selectedRole", role == null ? "" : role);

    return "admin/manage-users";
}


@PostMapping("/admin/users/{id}/role")
public String updateUserRole(
        @PathVariable Long id,
        @RequestParam String role,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {

    // Only allow these two roles
    if (!"USER".equals(role) && !"ADMIN".equals(role)) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", "Invalid role selected.");
        return "redirect:/admin/users";
    }

    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));

    // Prevent changing your own role
    if (user.getEmail().equalsIgnoreCase(authentication.getName())) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", "You cannot change your own role.");
        return "redirect:/admin/users";
    }

    // Do not demote the last remaining admin
    if ("ADMIN".equals(user.getRole()) && "USER".equals(role)) {
        long adminCount = userRepository.findAll().stream()
                .filter(u -> "ADMIN".equals(u.getRole()))
                .count();

        if (adminCount <= 1) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "The last admin account cannot be demoted.");
            return "redirect:/admin/users";
        }
    }

    user.setRole(role);
    userRepository.save(user);

    redirectAttributes.addFlashAttribute(
            "successMessage", "User role updated successfully.");

    return "redirect:/admin/users";
}


@PostMapping("/admin/trips/{id}/delete")
public String deleteTrip(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes) {

    Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Trip not found"));

    tripRepository.delete(trip);

    redirectAttributes.addFlashAttribute(
            "successMessage",
            "Trip deleted successfully.");

    return "redirect:/admin/users/" + trip.getUser().getId() + "/trips";
}


}
