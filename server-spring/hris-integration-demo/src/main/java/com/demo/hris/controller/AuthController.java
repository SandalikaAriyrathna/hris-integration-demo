package com.demo.hris.controller;

import java.util.Locale;
import java.util.Map;

import com.demo.hris.dto.EmployeeResponse;
import com.demo.hris.dto.LoginRequest;
import com.demo.hris.model.Employee;
import com.demo.hris.repository.EmployeeRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;

    public AuthController(
            AuthenticationManager authenticationManager,
            EmployeeRepository employeeRepository,
            SecurityContextRepository contextRepository,
            SessionAuthenticationStrategy sessionStrategy) {

        this.authenticationManager = authenticationManager;
        this.employeeRepository = employeeRepository;
        this.contextRepository = contextRepository;
        this.sessionStrategy = sessionStrategy;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(
            CsrfToken token) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "headerName", token.getHeaderName(),
                        "token", token.getToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {

        final Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            body.email().trim().toLowerCase(Locale.ROOT),
                            body.password()));
        } catch (AuthenticationException exception) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                            "message",
                            "Invalid credentials or account unavailable."));
        }

        Employee employee = requireActiveEmployee(authentication);

        sessionStrategy.onAuthentication(
                authentication,
                request,
                response);

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        contextRepository.saveContext(
                context,
                request,
                response);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "employee",
                        EmployeeResponse.from(employee)));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, EmployeeResponse>> me(
            Authentication authentication) {

        Employee employee = requireActiveEmployee(authentication);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "employee",
                        EmployeeResponse.from(employee)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        new SecurityContextLogoutHandler()
                .logout(request, response, authentication);

        new CookieClearingLogoutHandler("hris_demo_session")
                .logout(request, response, authentication);

        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private Employee requireActiveEmployee(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw unavailable();
        }

        return employeeRepository
                .findByEmailIgnoreCase(authentication.getName())
                .filter(Employee::isActive)
                .orElseThrow(this::unavailable);
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Your demo platform session is unavailable.");
    }
}