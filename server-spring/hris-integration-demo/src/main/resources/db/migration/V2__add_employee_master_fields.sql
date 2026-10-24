ALTER TABLE employees
    ADD COLUMN name_with_initials VARCHAR(255) NULL,
    ADD COLUMN origin_branch_id VARCHAR(255) NULL,
    ADD COLUMN branch_name VARCHAR(255) NULL,
    ADD COLUMN department VARCHAR(255) NULL,
    ADD COLUMN division VARCHAR(255) NULL,
    ADD COLUMN office_location VARCHAR(255) NULL,
    ADD COLUMN employment_type VARCHAR(100) NULL,
    ADD COLUMN hire_date DATE NULL,
    ADD COLUMN profile_image LONGTEXT NULL;

-- Fictional values for the existing demo employee.
UPDATE employees
SET name_with_initials = 'A. Silva',
    origin_branch_id = 'DEMO-BRANCH-001',
    branch_name = 'Demo Colombo Branch',
    department = 'Quantity Surveying',
    division = 'Consultancy',
    office_location = 'Colombo',
    employment_type = 'Permanent',
    hire_date = '2022-06-01'
WHERE employee_id = 'DEMO-EMP-001';