package com.demo.hris.controller;

import java.net.URI;
import java.util.UUID;

import com.demo.hris.model.Employee;
import com.demo.hris.repository.EmployeeRepository;
import com.demo.hris.service.ExcomIntegrationService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/excom")
public class ExcomIntegrationController {

    private final EmployeeRepository employeeRepository;
    private final ExcomIntegrationService integrationService;

    public ExcomIntegrationController(
            EmployeeRepository employeeRepository,
            ExcomIntegrationService integrationService) {

        this.employeeRepository = employeeRepository;
        this.integrationService = integrationService;
    }

    @GetMapping("/open")
    public ResponseEntity<Void> open(
            Authentication authentication) {

        requireActiveEmployee(authentication);

        return redirect(integrationService.getStartUrl());
    }

    @GetMapping("/continue")
    public ResponseEntity<Void> continueLogin(
            @RequestParam("requestId") UUID requestId,
            Authentication authentication) {

        Employee employee =
                requireActiveEmployee(authentication);

        URI launchUrl = integrationService.createLaunch(
                requestId,
                employee);

        return redirect(launchUrl);
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

    private ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .location(location)
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .build();
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Your demo platform session is unavailable.");
    }
}