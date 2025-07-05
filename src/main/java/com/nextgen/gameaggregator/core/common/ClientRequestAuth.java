package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.operator.constant.EndPoints;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ClientRequestAuth<T> {
    private final Integer agentId;
    private final T requestObject;
    private String apiKey;
    private String apiSecret;
    @Getter
    private String callback;
    private final Validator validator;

    public ClientRequestAuth(Integer agentId, T requestObject, Validator validator) {
        this.agentId = agentId;
        this.requestObject = requestObject;
        this.validator = validator;
        this.init();
    }

    private void init() {
        this.validateRequest(requestObject);
        this.loadClientCredentials();
    }

    private void validateRequest(T requestObject) {
        Set<ConstraintViolation<T>> violations = validator.validate(requestObject);
        if (!violations.isEmpty()) {
            // throw internal error
        }
    }

    private void loadClientCredentials() {

    }

    public Map<String, String> getHeaders() {
        String signature = SignatureGenerator.generate(this.requestObject, this.apiSecret);

        return Map.ofEntries(
                Map.entry(EndPoints.HEADER_API_KEY, this.apiKey),
                Map.entry(EndPoints.HEADER_SIGNATURE, signature)
        );
    }
}
