package com.tripnova.tripnova.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterForm {

	@NotBlank(message = "Please enter your full name")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String fullName;

    @NotBlank(message = "Please enter your email")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Email is too long")
    private String email;

    @NotBlank(message = "Please enter a password")
    @Size(min = 8, max = 72,
          message = "Password must be between 8 and 72 characters")
    private String password;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
