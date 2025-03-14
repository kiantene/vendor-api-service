package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.ga.writer.*;
import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ValidationService {
    private final AgentService agentService;
    private final AgentApiCredentialRepository agentApiCredentialRepository;
    private final AgentPlayerRepository agentPlayerRepository;
    private final VendorGameCodeRepository vendorGameCodeRepository;
    private final VendorGameCurrencyRepository vendorGameCurrencyRepository;
    private final AgentVendorLineRepository agentVendorLineRepository;
    private final VendorGameDeactivatedService vendorGameDeactivatedService;
    private final LoggingService loggingService;

    @Autowired
    public ValidationService(AgentService agentService,
                             AgentApiCredentialRepository agentApiCredentialRepository,
                             AgentPlayerRepository agentPlayerRepository,
                             VendorGameCodeRepository vendorGameCodeRepository,
                             VendorGameCurrencyRepository vendorGameCurrencyRepository,
                             AgentVendorLineRepository agentVendorLineRepository,
                             VendorGameDeactivatedService vendorGameDeactivatedService,
                             LoggingService loggingService) {

        this.agentService = agentService;
        this.agentApiCredentialRepository = agentApiCredentialRepository;
        this.agentPlayerRepository = agentPlayerRepository;
        this.vendorGameCodeRepository = vendorGameCodeRepository;
        this.vendorGameCurrencyRepository = vendorGameCurrencyRepository;
        this.agentVendorLineRepository = agentVendorLineRepository;
        this.vendorGameDeactivatedService = vendorGameDeactivatedService;
        this.loggingService = loggingService;
    }


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

    public void validateAgentStatus(Agent agent) throws AuthenticationException {
        if (!agent.getStatus().equals(Status.ACTIVE.code)) {
            throw new AuthenticationException();
        }
    }

    public void validateIsCustodianSeamlessAgentWalletType(Agent agent) throws InvalidWalletTypeException {
        if (!agent.getWalletType().equals(1)) {
            throw new InvalidWalletTypeException();
        } else if (!agent.getSeamlessType().equals(2)) {
            throw new InvalidWalletTypeException();
        }
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
        try {
            loggingService.logStart();
            Agent agent = agentService.get(agentVendorLines.getAgentId());
            // Add more values for checking
            vendorGameDeactivatedService.checkGameSupported(agent.getHouseId(), agent.getMasterAgentId(), agent.getId(), gameSession.getVendorGameId());
            loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ vendorGameDeactivatedService.checkGameSupported(" + agent + ","
                    + vendorGameCode.getId() + ")", gameSession.getVendorPlayerUsername(), "Eligible Bet No RoundId");
        } catch (AgentNotFoundException agentNotFoundException) {
            throw new AuthenticationException("Agent Id (" + agentVendorLines.getAgentId() + ") cannot be found");
        }
    }


    public void isBetAllowed(GameSession gameSession, String vendorUserName) throws
            InvalidPlayerException, DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException,
            AuthenticationException, GameTerminatedException {

        //1. verify is session terminated
        if (gameSession.getStatus().equals(0)) {
            throw new GameTerminatedException(gameSession.getVendorGameCode());
        }

        //2. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), vendorUserName, InvalidPlayerException::new);

        //3. verify agent Vendor line
        AgentVendorLine agentVendorLines = agentVendorLineRepository.
                findTop1ByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryIdAndStatus(
                        gameSession.getAgentId(), gameSession.getVendorId(), gameSession.getCurrencyId(),
                        gameSession.getGameCategoryId(), Status.ACTIVE.code);

        //vendor line not found
        Optional.ofNullable(agentVendorLines).orElseThrow(DisabledVendorLineException::new);

        //4. Verify Agent Player status
        AgentPlayer agentPlayer = agentPlayerRepository.
                findByAgentIdAndUsernameAndStatus(gameSession.getAgentId(), gameSession.getAgentPlayerUsername(), Status.ACTIVE.code);

        Optional.ofNullable(agentPlayer).orElseThrow(DisabledAgentPlayerException::new);

        //5. verify by vendor openGameCode instead, for play game with different game code token
        VendorGameCode vendorGameCode = vendorGameCodeRepository.findByOpenGameCodeAndPlatformIdAndLanguageIdAndStatusAndVendorId(gameSession.getVendorGameCode(),
                gameSession.getPlatformId(), gameSession.getLanguageId(), Status.ACTIVE.code, gameSession.getVendorId());

        Optional.ofNullable(vendorGameCode).orElseThrow(DisabledGameException::new);

        //6.  verify vendor Game status with currency
        VendorGameCurrency vendorGameCurrency = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyIdAndStatus(
                gameSession.getVendorGameId(), gameSession.getCurrencyId(), Status.ACTIVE.code);

        Optional.ofNullable(vendorGameCurrency).orElseThrow(DisabledGameException::new);

        //7.  verify game deactivated status for agent, master agent and house level
        try {
            Agent agent = agentService.get(agentVendorLines.getAgentId());
            vendorGameDeactivatedService.checkGameSupported(agent.getHouseId(), agent.getMasterAgentId(), agent.getId(), gameSession.getVendorGameId());

        } catch (AgentNotFoundException agentNotFoundException) {
            throw new AuthenticationException("Agent Id (" + agentVendorLines.getAgentId() + ") cannot be found");
        }
    }
}
