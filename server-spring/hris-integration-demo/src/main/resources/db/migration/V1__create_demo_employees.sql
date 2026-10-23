CREATE TABLE employees (
                           id VARCHAR(255) NOT NULL,
                           employee_id VARCHAR(255) NOT NULL,
                           employee_name VARCHAR(255) NOT NULL,
                           email VARCHAR(255) NOT NULL,
                           designation VARCHAR(255) NULL,
                           status INT NOT NULL DEFAULT 1,

                           password_hash VARCHAR(255) NOT NULL,

                           PRIMARY KEY (id),

                           CONSTRAINT uk_demo_employee_id UNIQUE (employee_id),
                           CONSTRAINT uk_demo_employee_email UNIQUE (email),

                           CONSTRAINT chk_demo_employee_status
                               CHECK (status IN (0, 1))
);