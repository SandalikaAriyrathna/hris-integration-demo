package com.demo.hris.repository;

import java.util.Optional;

import com.demo.hris.model.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository
        extends JpaRepository<Employee, String> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    Optional<Employee> findByEmployeeId(String employeeId);
}