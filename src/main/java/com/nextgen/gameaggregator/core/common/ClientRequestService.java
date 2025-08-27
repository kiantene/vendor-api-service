package com.nextgen.gameaggregator.core.common;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.entity.AgentApiCredential;
import com.nextgen.gameaggregator.core.exception.InternalValidationException;
import com.nextgen.gameaggregator.core.service.AgentApiCredentialDataService;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClientRequestService {
    @Value("${testing.stub-prefix:stub}")
    private String usernamePrefix;
    private final Validator validator;
    private final AgentApiCredentialDataService credentialService;

    public ClientRequestService(Validator validator, AgentApiCredentialDataService credentialService) {
        this.validator = validator;
        this.credentialService = credentialService;
    }

    public <T> ClientApiRequest<T> createClientApiRequest(Integer agentId, String path, T requestObject) {
        validateInputs(agentId, path, requestObject);
        validateRequestObject(requestObject);

        AgentApiCredential credential = loadCredential(agentId);

        return ClientApiRequest.<T>builder()
                .agentId(agentId)
                .path(path)
                .requestObject(requestObject)
                .credential(credential)
                .build();
    }

    public boolean shouldMockResponse(String username) {
        return username.toLowerCase().startsWith(usernamePrefix.toLowerCase());
    }

    public ClientBalanceResponse mockClientResponse(String traceId, String currency, String username) {
        PlayerBalanceData playerBalanceData = new PlayerBalanceData(
                username,
                currency,
                BigDecimal.ONE,
                System.currentTimeMillis()
        );

        ClientBalanceResponse response = new ClientBalanceResponse();
        response.setStatus(ResponseCodes.Status.SC_OK.toString());
        response.setTraceId(traceId);
        response.setMessage(ResponseCodes.Status.SC_OK.description + " mock response");
        response.setData(playerBalanceData);
        return response;
    }

    private void validateInputs(Integer agentId, String path, Object requestObject) {
        if (agentId == null) {
            throw new InternalValidationException("Agent ID cannot be null");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new InternalValidationException("Path cannot be null or empty");
        }
        if (requestObject == null) {
            throw new InternalValidationException("Request object cannot be null");
        }
    }

    private <T> void validateRequestObject(T requestObject) {
        Set<ConstraintViolation<T>> violations = validator.validate(requestObject);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> String.format("%s: %s", v.getPropertyPath(), v.getMessage()))
                    .collect(Collectors.joining(", "));

            throw new InternalValidationException("Request validation failed: " + errorMessage);
        }
    }

    private AgentApiCredential loadCredential(Integer agentId) {
        AgentApiCredential credential = credentialService.getActiveCredential(agentId);

        if (credential == null) {
            throw new InternalConfigurationException(
                    String.format("No active credentials found for agent ID: %d", agentId)
            );
        }

        // Validate required fields
        requireNonEmpty(credential.getApiKey(), "API key", agentId);
        requireNonEmpty(credential.getApiSecret(), "API secret", agentId);
        requireNonEmpty(credential.getCallbackUrl(), "callback URL", agentId);

        return credential;
    }

    private void requireNonEmpty(String value, String fieldName, Integer agentId) {
        if (value == null || value.trim().isEmpty()) {
            throw new InternalConfigurationException(
                    String.format("%s is empty for agent ID: %d", fieldName, agentId)
            );
        }
    }
}
