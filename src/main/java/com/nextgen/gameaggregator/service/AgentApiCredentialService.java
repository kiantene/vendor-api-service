package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.AgentCurrency;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidUrlException;
import com.nextgen.gameaggregator.operator.apiverification.agentinfo.AgentInfoVo;
import com.nextgen.gameaggregator.repository.ga.writer.AgentApiCredentialRepository;
import com.nextgen.gameaggregator.repository.ga.writer.AgentCurrencyRepository;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AgentApiCredentialService {
    @Autowired
    private AgentApiCredentialRepository agentApiCredentialRepository;
    @Autowired
    private AgentCurrencyRepository agentCurrencyRepository;

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

    public AgentInfoVo getAgentApiCredentialForIntegrationTest(Integer agentId, AgentInfoVo agentInfoVo) throws InvalidAgentApiCredentialException {

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

        agentInfoVo.setApiSecret(credential.getApiSecret());
        agentInfoVo.setApiKey(credential.getApiKey());
        agentInfoVo.setCurrency(credential.getAgent().getCurrency().getCode());
        return agentInfoVo;
    }

    public List<AgentCurrency> getAgentSupportedCurrency(Integer agentId)  throws CurrencyNotSupportedException {
        List<AgentCurrency> agentCurrencies = agentCurrencyRepository.findAgentCurrencyByAgentIdAndStatus(agentId, Status.ACTIVE.code );

        if (agentCurrencies.isEmpty()) {
            throw new CurrencyNotSupportedException();
        }

        return agentCurrencies;

    }
}
