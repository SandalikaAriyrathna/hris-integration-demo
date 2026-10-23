package com.demo.hris.dto;

import com.demo.hris.model.Employee;

public record EmployeeResponse(
        String employeeId,
        String firstName,
        String lastName,
        String email,
        String designation
) {

    public static EmployeeResponse from(Employee employee) {
        String[] names = employee.getEmployeeName()
                .trim()
                .split("\\s+", 2);

        String firstName = names[0];
        String lastName = names.length > 1 ? names[1] : "";

        return new EmployeeResponse(
                employee.getEmployeeId(),
                firstName,
                lastName,
                employee.getEmail(),
                employee.getDesignation());
    }
}