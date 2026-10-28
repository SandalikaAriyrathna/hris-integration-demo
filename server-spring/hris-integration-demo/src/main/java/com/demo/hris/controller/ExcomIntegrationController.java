package com.demo.hris.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.demo.hris.model.Employee;
import com.demo.hris.repository.EmployeeRepository;
import com.demo.hris.service.ExcomIntegrationService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

        try {
            requireActiveEmployee(authentication);
            return redirect(integrationService.getStartUrl());
        } catch (ResponseStatusException exception) {
            return redirectToHris(exception.getReason());
        }
    }

    @GetMapping("/continue")
    public ResponseEntity<Void> continueLogin(
            @RequestParam("requestId") UUID requestId,
            Authentication authentication) {

        try {
            Employee employee = requireActiveEmployee(authentication);

            URI launchUrl = integrationService.createLaunch(
                    requestId,
                    employee);

            return redirect(launchUrl);
        } catch (ResponseStatusException exception) {
            return redirectToHris(exception.getReason());
        }
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Void> invalidIntegrationRequest() {
        return redirectToHris(
                "The EXCOM login request is invalid. "
                        + "Please use Open Resume Builder to start again.");
    }

    private Employee requireActiveEmployee(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw unavailable();
        }

        return employeeRepository
                .findByEmailIgnoreCase(authentication.getName())
                .filter(Employee::isActive)
                .orElseThrow(this::unavailable);
    }

    private ResponseEntity<Void> redirectToHris(String reason) {
        String message = reason == null || reason.isBlank()
                ? "EXCOM could not be opened. Please try again."
                : reason.replaceAll("\\p{Cntrl}", " ").trim();

        if (message.length() > 500) {
            message = message.substring(0, 500);
        }

        String encoded = URLEncoder.encode(
                message,
                StandardCharsets.UTF_8);

        return redirect(URI.create("/?excomError=" + encoded));
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
                "Your HRIS session is unavailable. Please sign in again.");
    }
}