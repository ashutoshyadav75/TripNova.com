package com.tripnova.tripnova.Config;




import com.tripnova.tripnova.Repository.UserRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class SecurityConfig {
	

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            UserRepository userRepository) {

        return email -> userRepository.findByEmailIgnoreCase(email)
                .map(user ->
                        org.springframework.security.core.userdetails.User
                                .withUsername(user.getEmail())
                                .password(user.getPassword())
                                .roles(user.getRole())
                                .build()
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User account not found"
                        )
                );
    }


@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http) throws Exception {

    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/", "/login", "/register",
                "/css/**", "/js/**", "/images/**",
                "/favicon.ico", "/error"
            ).permitAll()

            .requestMatchers("/admin/**").hasRole("ADMIN")

            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .usernameParameter("email")
            .passwordParameter("password")
            .successHandler((request, response, authentication) -> {
                boolean isAdmin = authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

                if (isAdmin) {
                    response.sendRedirect("/admin/dashboard");
                } else {
                    response.sendRedirect("/trips");
                }
            })
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        );

    return http.build();
}

}

