package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.*;
import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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
    @Autowired
    private LoggingService loggingService;
    @Autowired
    private VendorGameDeactivatedService vendorGameDeactivatedService;

    @Cacheable(value = "AgentApiCredentialsByApiKey", key = "#apiKey", cacheManager = "cacheManager")
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
        loggingService.logStart();
        AgentVendorLine agentVendorLines = agentVendorLineRepository.
                findTop1ByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryIdAndStatus(
                        gameSession.getAgentId(), gameSession.getVendorId(), gameSession.getCurrencyId(),
                        gameSession.getGameCategoryId(), Status.ACTIVE.code);
        loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ agentVendorLineRepository.findTop1ByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryIdAndStatus(" + gameSession.getAgentId() + ","
                + gameSession.getVendorId() + "," + gameSession.getCurrencyId() + "," + gameSession.getGameCategoryId() + "," + Status.ACTIVE.code + ")",
                gameSession.getVendorPlayerUsername(), "Eligible Bet No RoundId");
        //vendor line not found
        Optional.ofNullable(agentVendorLines).orElseThrow(DisabledVendorLineException::new);

        //4. Verify Agent Player status
        loggingService.logStart();
        AgentPlayer agentPlayer = agentPlayerRepository.
                findByAgentIdAndUsernameAndStatus(gameSession.getAgentId(), gameSession.getAgentPlayerUsername(), Status.ACTIVE.code);
        loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ agentPlayerRepository.findByAgentIdAndUsernameAndStatus(" + gameSession.getAgentId() + ","
                        + gameSession.getAgentPlayerUsername() + Status.ACTIVE.code + ")", gameSession.getVendorPlayerUsername(), "Eligible Bet No RoundId");
        Optional.ofNullable(agentPlayer).orElseThrow(DisabledAgentPlayerException::new);

        //5. verify by vendor openGameCode instead, for play game with different game code token
        loggingService.logStart();
        VendorGameCode vendorGameCode = vendorGameCodeRepository.findByOpenGameCodeAndPlatformIdAndLanguageIdAndStatusAndVendorId(gameSession.getVendorGameCode(),
                gameSession.getPlatformId(), gameSession.getLanguageId(), Status.ACTIVE.code, gameSession.getVendorId());
        loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ vendorGameCodeRepository.findByOpenGameCodeAndPlatformIdAndLanguageIdAndStatusAndVendorId(" + gameSession.getVendorGameCode() + ","
                        + gameSession.getPlatformId() + "," + gameSession.getLanguageId() + "," + Status.ACTIVE.code + "," + gameSession.getVendorId() + ")",
                gameSession.getVendorPlayerUsername(), "Eligible Bet No RoundId");
        Optional.ofNullable(vendorGameCode).orElseThrow(DisabledGameException::new);

        //6.  verify vendor Game status with currency
        loggingService.logStart();
        VendorGameCurrency vendorGameCurrency = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyIdAndStatus(
                gameSession.getVendorGameId(), gameSession.getCurrencyId(), Status.ACTIVE.code);
        loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyIdAndStatus(" + gameSession.getVendorGameId() + ","
                + gameSession.getCurrencyId() + Status.ACTIVE.code + ")", gameSession.getVendorPlayerUsername(), "Eligible Bet No RoundId");
        Optional.ofNullable(vendorGameCurrency).orElseThrow(DisabledGameException::new);

        //7.  verify game deactivated status for agent, master agent and house level
        loggingService.logStart();
        vendorGameDeactivatedService.checkGameSupported(agentVendorLines.getAgent(), gameSession.getVendorGameId());
        loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ vendorGameDeactivatedService.checkGameSupported(" + agentVendorLines.getAgent() + ","
                + vendorGameCode.getId() + ")", gameSession.getVendorPlayerUsername(), "Eligible Bet No RoundId");
    }
}
