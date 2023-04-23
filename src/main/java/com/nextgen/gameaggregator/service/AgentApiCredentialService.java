package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidUrlException;
import com.nextgen.gameaggregator.repository.AgentApiCredentialRepository;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AgentApiCredentialService {
    @Autowired
    private AgentApiCredentialRepository agentApiCredentialRepository;

    @Cacheable(value = "AgentApiCredentials", key = "#agentId", cacheManager = "cacheManager")
    public AgentApiCredential getAgentApiCredential(Integer agentId) throws InvalidAgentApiCredentialException {

        AgentApiCredential credential = agentApiCredentialRepository.findByAgentIdAndStatus(agentId, Status.ACTIVE.code);
        Optional.ofNullable(credential).orElseThrow(InvalidAgentApiCredentialException::new);
        //UPDATE PG : TEMP WHITELIST LOCALHOST
        try{
            if(!credential.getCallbackUrl().equals("http://localhost:8087/api/v2")){
                ValidationUtils.isValidUrl(credential.getCallbackUrl());
            }
        }catch (InvalidUrlException invalidUrlException){
            throw new InvalidAgentApiCredentialException();
        }

        return credential;
    }
}
