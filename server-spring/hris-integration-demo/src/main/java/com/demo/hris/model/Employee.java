package com.demo.hris.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

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

    @Column(name = "name_with_initials", length = 255)
    private String nameWithInitials;

    @Column(name = "origin_branch_id", length = 255)
    private String originBranchId;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(length = 255)
    private String department;

    @Column(length = 255)
    private String division;

    @Column(name = "office_location", length = 255)
    private String officeLocation;

    @Column(name = "employment_type", length = 100)
    private String employmentType;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "profile_image", columnDefinition = "LONGTEXT")
    private String profileImage;

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

    public String getNameWithInitials() {
        return nameWithInitials;
    }

    public String getOriginBranchId() {
        return originBranchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getDepartment() {
        return department;
    }

    public String getDivision() {
        return division;
    }

    public String getOfficeLocation() {
        return officeLocation;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public boolean isActive() {
        return Integer.valueOf(1).equals(status);
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}