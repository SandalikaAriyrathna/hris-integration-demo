package com.demo.hris.service;

import java.util.Locale;

import com.demo.hris.model.Employee;
import com.demo.hris.repository.EmployeeRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public EmployeeUserDetailsService(
            EmployeeRepository employeeRepository) {

        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        if (email == null || email.isBlank()) {
            throw unavailable();
        }

        Employee employee = employeeRepository
                .findByEmailIgnoreCase(
                        email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(this::unavailable);

        return User.withUsername(employee.getEmail())
                .password(employee.getPasswordHash())
                .disabled(!employee.isActive())
                .roles("EMPLOYEE")
                .build();
    }

    private UsernameNotFoundException unavailable() {
        return new UsernameNotFoundException(
                "Invalid credentials or account unavailable.");
    }
}