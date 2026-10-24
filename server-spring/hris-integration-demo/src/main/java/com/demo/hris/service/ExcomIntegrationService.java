package com.demo.hris.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.demo.hris.dto.EmployeeResponse;
import com.demo.hris.model.Employee;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExcomIntegrationService {

    private final RestClient restClient;
    private final URI frontendUrl;
    private final String clientId;
    private final String apiKey;

    public ExcomIntegrationService(
            @Value("${app.excom.backend-url}") String backendUrl,
            @Value("${app.excom.frontend-url}") String frontendUrl,
            @Value("${app.excom.client-id}") String clientId,
            @Value("${app.excom.api-key}") String apiKey) {

        URI backend = validateOrigin(backendUrl);
        this.frontendUrl = validateOrigin(frontendUrl);

        if (!clientId.matches("[A-Za-z0-9_-]{1,100}")
                || !apiKey.matches("[A-Fa-f0-9]{64,256}")) {

            throw new IllegalArgumentException(
                    "Configure valid EXCOM integration credentials.");
        }

        this.clientId = clientId;
        this.apiKey = apiKey;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(backend.toASCIIString())
                .requestFactory(factory)
                .build();
    }

    public URI getStartUrl() {
        return frontendUrl.resolve("/api/integrations/sso/start");
    }

    public URI createLaunch(UUID requestId, Employee employee) {
        if (!employee.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This demo employee account is unavailable.");
        }

        EmployeeResponse identity = EmployeeResponse.from(employee);

        if (identity.lastName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The employee needs an approved first-name and last-name mapping.");
        }

        EmployeeMasterData master = new EmployeeMasterData(
                employee.getNameWithInitials(),
                employee.getOriginBranchId(),
                employee.getBranchName(),
                employee.getDepartment(),
                employee.getDivision(),
                employee.getOfficeLocation(),
                employee.getEmploymentType(),
                employee.getHireDate(),
                employee.getProfileImage(),
                Set.of()
        );

        LaunchRequest body = new LaunchRequest(
                requestId,
                identity.employeeId(),
                identity.firstName(),
                identity.lastName(),
                identity.email(),
                identity.designation(),
                master
        );

        final LaunchResponse result;

        try {
            result = restClient.post()
                    .uri("/api/integrations/sso/launch")
                    .header("X-Integration-Client", clientId)
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(LaunchResponse.class);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            if (status == 403 || status == 409) {
                throw new ResponseStatusException(
                        HttpStatus.valueOf(status),
                        "EXCOM could not link or authorize this employee. "
                                + "Contact an administrator.");
            }

            throw unavailable();
        } catch (RestClientException exception) {
            throw unavailable();
        }

        return validateLaunchUrl(result, requestId);
    }

    private URI validateLaunchUrl(
            LaunchResponse result,
            UUID requestId) {

        if (result == null || result.launchUrl() == null) {
            throw unavailable();
        }

        final URI launch;

        try {
            launch = URI.create(result.launchUrl());
        } catch (IllegalArgumentException exception) {
            throw unavailable();
        }

        String expectedQueryPrefix =
                "requestId=" + requestId + "&code=";

        String query = launch.getRawQuery();

        if (!frontendUrl.getScheme().equalsIgnoreCase(
                launch.getScheme())
                || !frontendUrl.getRawAuthority().equalsIgnoreCase(
                launch.getRawAuthority())
                || !"/api/integrations/sso/complete".equals(
                launch.getRawPath())
                || launch.getRawUserInfo() != null
                || launch.getRawFragment() != null
                || query == null
                || !query.startsWith(expectedQueryPrefix)
                || !query.substring(expectedQueryPrefix.length())
                .matches("[A-Za-z0-9_-]{43}")) {

            throw unavailable();
        }

        return launch;
    }

    private URI validateOrigin(String value) {
        final URI uri;

        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Configure a valid EXCOM URL.");
        }

        String host = uri.getHost();

        boolean https =
                "https".equalsIgnoreCase(uri.getScheme());

        boolean localHttp =
                "http".equalsIgnoreCase(uri.getScheme())
                        && host != null
                        && Set.of("localhost", "127.0.0.1", "[::1]")
                        .contains(host.toLowerCase(
                                java.util.Locale.ROOT));

        String path = uri.getRawPath();

        if (host == null
                || (!https && !localHttp)
                || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || (path != null
                && !path.isEmpty()
                && !"/".equals(path))) {

            throw new IllegalArgumentException(
                    "EXCOM URLs must be HTTPS origins, "
                            + "except for local development.");
        }

        return uri;
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "EXCOM login could not be completed. "
                        + "Check the integration configuration and try again.");
    }

    public record LaunchRequest(
            UUID requestId,
            String employeeId,
            String firstName,
            String lastName,
            String email,
            String designation,
            EmployeeMasterData employeeMaster) {
    }

    public record EmployeeMasterData(
            String nameWithInitials,
            String branchId,
            String branchName,
            String department,
            String division,
            String officeLocation,
            String employmentType,
            LocalDate joiningDate,
            String photoBase64,
            Set<String> clearFields) {
    }

    public record LaunchResponse(String launchUrl) {
    }
}