package com.nextgen.gameaggregator.core.common;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.config.properties.WalletServiceProperties;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.AgentApiCredential;
import com.nextgen.gameaggregator.core.exception.InternalValidationException;
import com.nextgen.gameaggregator.core.service.AgentApiCredentialDataService;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.webclient.ClientApiRequest;
import com.nextgen.gameaggregator.enums.SeamlessType;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@EnableConfigurationProperties(WalletServiceProperties.class)
public class ClientRequestService {
    private final Validator validator;
    private final AgentApiCredentialDataService credentialService;
    private final AgentDataService agentService;
    private final WalletServiceProperties props;
    @Value("${testing.stub-prefix:stub}")
    private String usernamePrefix;

    public ClientRequestService(Validator validator,
                                AgentApiCredentialDataService credentialService,
                                AgentDataService agentService,
                                WalletServiceProperties props) {
        this.validator = validator;
        this.credentialService = credentialService;
        this.agentService = agentService;
        this.props = props;
    }

    public <T> ClientApiRequest<T> createClientApiRequest(String traceId,
                                                          Integer agentId,
                                                          String path,
                                                          T requestObject) {
        validateInputs(agentId, path, requestObject);
        validateRequestObject(requestObject);

        AgentApiCredential credential = loadCredential(agentId);
        Agent agent = agentService.get(agentId);

        String baseUrl = credential.getCallbackUrl();
        if (SeamlessType.SEAMLESS_TRANSFER.code.equals(agent.getSeamlessType())) {
            baseUrl = props.getHost() + "/seamless";
        }

        return ClientApiRequest.<T>builder()
                .traceId(traceId)
                .agentId(agentId)
                .method(HttpMethod.POST)
                .baseUrl(baseUrl)
                .path(path)
                .requestObject(requestObject)
                .apiKey(credential.getApiKey())
                .apiSecret(credential.getApiSecret())
                .build();
    }

    public boolean shouldMockResponse(String username) {
        return username.toLowerCase().startsWith(usernamePrefix.toLowerCase());
    }

    public ClientApiResponse mockClientResponse(String traceId, String currency, String username) {
        PlayerBalanceData playerBalanceData = new PlayerBalanceData(
                username,
                currency,
                BigDecimal.ONE,
                System.currentTimeMillis()
        );

        ClientApiResponse response = new ClientApiResponse();
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
