package com.demo.hris.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @Column(nullable = false, length = 255)
    private String id;

    @Column(name = "employee_id", nullable = false, unique = true,
            length = 255)
    private String employeeId;

    @Column(name = "employee_name", nullable = false, length = 255)
    private String employeeName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String designation;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    protected Employee() {
        // Required by JPA.
    }

    public Employee(
            String id,
            String employeeId,
            String employeeName,
            String email,
            String designation,
            String passwordHash) {

        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.email = email;
        this.designation = designation;
        this.status = 1;
        this.passwordHash = passwordHash;
    }

    public String getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getEmail() {
        return email;
    }

    public String getDesignation() {
        return designation;
    }

    public boolean isActive() {
        return Integer.valueOf(1).equals(status);
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}