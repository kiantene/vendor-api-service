package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.repository.AgentApiCredentialRepository;
import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ValidationService {
    @Autowired
    private AgentApiCredentialRepository agentApiCredentialRepository;

    public AgentApiCredential validateApiKey(String apiKey) throws AuthenticationException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new AuthenticationException();
        }

        AgentApiCredential entity = agentApiCredentialRepository.findByApiKey(apiKey);

        if (entity == null || entity.getStatus() != 1) {
            throw new AuthenticationException();
        }

        return entity;
    }

    public void validateSignature(String payload, String secret, String signature) throws InvalidSignatureException {
        if (signature == null || signature.isEmpty()) {
            throw new InvalidSignatureException();
        }

        String actualSignature = ApiSecurityUtils.getHmacSignature(payload, secret);
        log.info(payload);
        log.info(secret);
        log.info(actualSignature);

        if (!signature.equals(actualSignature)) {
            throw new InvalidSignatureException();
        }
    }
}
