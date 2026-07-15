package com.nextgen.gameaggregator.core.common;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.config.properties.WalletServiceProperties;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.context.OperatorRequestContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorScenario;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.AgentApiCredential;
import com.nextgen.gameaggregator.core.exception.InternalValidationException;
import com.nextgen.gameaggregator.core.service.AgentApiCredentialDataService;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.util.OperatorSignatureUtil;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.enums.SeamlessType;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@EnableConfigurationProperties(WalletServiceProperties.class)
public class ClientRequestService {
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private final Validator validator;
    private final AgentApiCredentialDataService credentialService;
    private final AgentDataService agentService;
    private final WalletServiceProperties transferWalletProps;
    @Value("${testing.stub-prefix:stub}")
    private String usernamePrefix;

    public ClientRequestService(Validator validator,
                                AgentApiCredentialDataService credentialService,
                                AgentDataService agentService,
                                WalletServiceProperties transferWalletProps) {
        this.validator = validator;
        this.credentialService = credentialService;
        this.agentService = agentService;
        this.transferWalletProps = transferWalletProps;
    }

    public OperatorApiRequest createOperatorApiRequest(String traceId,
                                                       Integer agentId,
                                                       String agentPlayerUsername,
                                                       String path,
                                                       Object requestObject,
                                                       Long transactionTime) {
        validateInputs(agentId, path, requestObject);
        validateRequestObject(requestObject);

        ResolvedCredential resolved = loadCredential(agentId);
        AgentApiCredential credential = resolved.credential();
        stripRollbackMetaForNonTransferWallet(requestObject, resolved.transferWallet());

        return OperatorApiRequest.builder()
                .traceId(traceId)
                .agentId(agentId)
                .agentPlayerUsername(agentPlayerUsername)
                .method(HttpMethod.POST)
                .baseUrl(credential.getCallbackUrl())
                .path(path)
                .body(requestObject)
                .headers(getHeaders(credential, requestObject))
                .transactionTime(transactionTime)
                .build();
    }

    public OperatorApiRequest createOperatorApiRequest(OperatorRequestContext<OperatorRequestObject, OperatorScenario> context) {
        Integer agentId = context.round().getAgentMeta().getAgentId();

        validateInputs(agentId, context.endpoint(), context.request());
        validateRequestObject(context.request());

        ResolvedCredential resolved = loadCredential(agentId);
        AgentApiCredential credential = resolved.credential();
        stripRollbackMetaForNonTransferWallet(context.request(), resolved.transferWallet());

        return OperatorApiRequest.builder()
                .traceId(context.request().getTraceId())
                .agentId(agentId)
                .agentPlayerUsername(context.request().getUsername())
                .method(HttpMethod.POST)
                .baseUrl(credential.getCallbackUrl())
                .path(context.endpoint())
                .body(context.request())
                .headers(getHeaders(credential, context.request()))
                .transactionTime(context.request().getTimestamp())
                .timeout(context.timeoutInMillis())
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

    private Map<String, String> getHeaders(AgentApiCredential credential, Object requestObject) {
        return Map.of(
                HEADER_API_KEY, credential.getApiKey(),
                HEADER_SIGNATURE, OperatorSignatureUtil.sign(requestObject, credential.getApiSecret())
        );
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

    /**
     * RollbackMeta (operator-POV amounts) is only meant for the internal transfer wallet. The gate
     * is the authoritative seamless-transfer flag (agent's seamlessType == SEAMLESS_TRANSFER),
     * resolved in {@link #loadCredential}: for any other operator we drop the meta, since operators
     * decide their own reversal and must not receive an amount.
     */
    void stripRollbackMetaForNonTransferWallet(Object requestObject, boolean transferWallet) {
        if (!transferWallet && requestObject instanceof WalletRollbackDto rollbackDto) {
            rollbackDto.setMeta(null);
        }
    }

    /** A loaded credential together with whether its agent is a seamless-transfer (transfer wallet) agent. */
    private record ResolvedCredential(AgentApiCredential credential, boolean transferWallet) {}

    private ResolvedCredential loadCredential(Integer agentId) {
        AgentApiCredential credential = credentialService.getActiveCredential(agentId);

        if (credential == null) {
            throw new InternalConfigurationException(
                    String.format("No active credentials found for agent ID: %d", agentId)
            );
        }

        // The credential carries its agent (eager @ManyToOne, cached with it), so reuse it and avoid
        // a second cache lookup. Fall back to the agent cache only if the FK is absent (optional).
        Agent agent = credential.getAgent() != null ? credential.getAgent() : agentService.get(agentId);
        boolean transferWallet = SeamlessType.isSeamlessTransfer(agent.getSeamlessType());
        if (transferWallet) {
            credential.setCallbackUrl(transferWalletProps.getCallbackUrl());
        }

        // Validate required fields
        requireNonEmpty(credential.getApiKey(), "API key", agentId);
        requireNonEmpty(credential.getApiSecret(), "API secret", agentId);
        requireNonEmpty(credential.getCallbackUrl(), "callback URL", agentId);

        return new ResolvedCredential(credential, transferWallet);
    }

    private void requireNonEmpty(String value, String fieldName, Integer agentId) {
        if (value == null || value.trim().isEmpty()) {
            throw new InternalConfigurationException(
                    String.format("%s is empty for agent ID: %d", fieldName, agentId)
            );
        }
    }
}
