package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.InternalValidationException;
import com.nextgen.gameaggregator.core.service.data.AgentApiCredentialDataService;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClientRequestAuth<T> {
    @Getter
    private Integer agentId;
    @Getter
    private T requestObject;
    @Getter
    private String apiKey;
    private String apiSecret;
    @Getter
    private String baseUrl;
    @Getter
    private String path;
    private final Validator validator;
    private final AgentApiCredentialDataService credentialService;

    public ClientRequestAuth(Validator validator, AgentApiCredentialDataService credentialService) {
        this.validator = validator;
        this.credentialService = credentialService;
    }

    public void initialise(Integer agentId, String path, T requestObject) throws
            InternalValidationException, InternalConfigurationException {

        this.agentId = agentId;
        this.path = path;
        this.requestObject = requestObject;
        this.validateRequest();
        this.loadClientCredentials();
    }

    private void validateRequest() throws InternalValidationException {
        if (requestObject == null) {
            throw new InternalValidationException(this.getClass().getName() + ": validateRequest -> requestObject is null");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(requestObject);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            throw new InternalValidationException("Validation failed: " + errorMessage);
        }
    }

    private void loadClientCredentials() throws InternalValidationException, InternalConfigurationException {

        String logPrefix = this.getClass().getName() + ": loadClientCredentials -> ";

        if (agentId == null) {
            throw new InternalValidationException(logPrefix + "agentId is null");
        }

        AgentApiCredential credential = credentialService.getActiveCredential(agentId);
        if (credential == null)
            throw new InternalConfigurationException(logPrefix + "cannot find AgentApiCredential for agentId: " + agentId);

        apiKey = requireNonEmpty(credential.getApiKey(), logPrefix + "apiKey is empty for agentId: " + agentId);
        apiSecret = requireNonEmpty(credential.getApiSecret(), logPrefix + "apiSecret is empty for agentId: " + agentId);
        baseUrl = requireNonEmpty(credential.getCallbackUrl(), logPrefix + "callback is empty for agentId: " + agentId);
    }

    public Map<String, String> getHeaders() {
        String signature = SignatureGenerator.generate(this.requestObject, this.apiSecret);

        return Map.ofEntries(
                Map.entry(EndPoints.HEADER_API_KEY, this.apiKey),
                Map.entry(EndPoints.HEADER_SIGNATURE, signature)
        );
    }

    private String requireNonEmpty(String value, String errorMessage) {
        if (value == null || value.isEmpty()) {
            throw new InternalConfigurationException(errorMessage);
        }
        return value;
    }
}
