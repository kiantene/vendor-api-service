package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.*;
import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ValidationService {
    @Autowired
    private AgentApiCredentialRepository agentApiCredentialRepository;

    @Autowired
    private AgentPlayerRepository agentPlayerRepository;
    @Autowired
    private VendorGameCodeRepository vendorGameCodeRepository;
    @Autowired
    private VendorGameCurrencyRepository vendorGameCurrencyRepository;
    @Autowired
    private AgentVendorLineRepository agentVendorLineRepository;

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
        if (!signature.equals(actualSignature)) {
            throw new InvalidSignatureException();
        }
    }

    public void validateEligibleBet(GameSession gameSession, String vendorUserName) throws
            InvalidPlayerException, DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException,
            AuthenticationException {

        //1. verify is session terminated
        if (gameSession.getStatus().equals(0)) {
            throw new AuthenticationException();
        }

        //2. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), vendorUserName, InvalidPlayerException::new);

        //3. verify agent Vendor line
        List<AgentVendorLine> agentVendorLines = agentVendorLineRepository.
                findByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryIdAndStatus(
                        gameSession.getAgentId(), gameSession.getVendorId(), gameSession.getCurrencyId(),
                        gameSession.getGameCategoryId(), Status.ACTIVE.code);
        //vendor line not found
        if (agentVendorLines.isEmpty()) {
            throw new DisabledVendorLineException();
        }

        //4. Verify Agent Player status
        AgentPlayer agentPlayer = agentPlayerRepository.
                findByAgentIdAndUsernameAndStatus(gameSession.getAgentId(), gameSession.getAgentPlayerUsername(), Status.ACTIVE.code);
        Optional.ofNullable(agentPlayer).orElseThrow(DisabledAgentPlayerException::new);

        //5. verify vendor Game status with platform and language
        VendorGameCode vendorGameCode = vendorGameCodeRepository.
                findByVendorGameIdAndPlatformIdAndLanguageIdAndStatus(gameSession.getVendorGameId(),
                        gameSession.getPlatformId(), gameSession.getLanguageId(), Status.ACTIVE.code);
        Optional.ofNullable(vendorGameCode).orElseThrow(DisabledGameException::new);

        //6.  verify vendor Game status with currency
        VendorGameCurrency vendorGameCurrency = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyIdAndStatus(
                gameSession.getVendorGameId(), gameSession.getCurrencyId(), Status.ACTIVE.code);
        Optional.ofNullable(vendorGameCurrency).orElseThrow(DisabledGameException::new);
    }
}
