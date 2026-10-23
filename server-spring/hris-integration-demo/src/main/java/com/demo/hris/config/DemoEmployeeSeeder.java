package com.demo.hris.config;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.demo.hris.model.Employee;
import com.demo.hris.repository.EmployeeRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "app.demo.seed-enabled",
        havingValue = "true"
)
public class DemoEmployeeSeeder implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DemoEmployeeSeeder.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DemoEmployeeSeeder(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${DEMO_EMPLOYEE_PASSWORD:}") String password) {

        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (employeeRepository
                .findByEmployeeId("DEMO-EMP-001")
                .isPresent()) {

            log.info("Demo employee already exists; left unchanged.");
            return;
        }

        if (password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new IllegalArgumentException(
                    "Configure a demo password of at least 12 characters "
                            + "and no more than 72 UTF-8 bytes.");
        }

        employeeRepository.saveAndFlush(
                new Employee(
                        UUID.randomUUID().toString(),
                        "DEMO-EMP-001",
                        "Asha Silva",
                        "asha.demo@example.com",
                        "Quantity Surveyor",
                        passwordEncoder.encode(password)));

        log.info("Fictional demo employee created.");
    }
}