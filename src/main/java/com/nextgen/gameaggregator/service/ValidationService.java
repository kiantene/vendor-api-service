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
    private VendorLineService vendorLineService;
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

    public void validateIllegibleBet(RawGameSession rawGameSession, String vendorUserName ) throws
            InvalidPlayerException, DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException {

        //1. Verify received username is the same from game session
        ValidationUtils.isEquals(rawGameSession.getVendorPlayerUsername(), vendorUserName, InvalidPlayerException::new);

        //2. verify agent Vendor line
        List<AgentVendorLine> agentVendorLines = agentVendorLineRepository.
                findByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryIdAndStatus(
                        rawGameSession.getAgentId() , rawGameSession.getVendorId(), rawGameSession.getCurrencyId(),
                        rawGameSession.getGameCategoryId(), Status.ACTIVE.code);
        //vendor line not found
        if (agentVendorLines.isEmpty()) {
            throw new DisabledVendorLineException();
        }

        //3. Verify Agent Player status
        AgentPlayer agentPlayer = agentPlayerRepository.
                findByAgentIdAndUsernameAndStatus(rawGameSession.getAgentId(), rawGameSession.getAgentPlayerUsername(), Status.ACTIVE.code);
        Optional.ofNullable(agentPlayer).orElseThrow(() -> new DisabledAgentPlayerException());

        //4. verify vendor Game status with platform and language
        VendorGameCode vendorGameCode = vendorGameCodeRepository.
                findByVendorGameIdAndPlatformIdAndLanguageIdAndStatus(rawGameSession.getVendorGameId(),
                        rawGameSession.getPlatformId(), rawGameSession.getLanguageId(), Status.ACTIVE.code);
        Optional.ofNullable(vendorGameCode).orElseThrow(() -> new DisabledGameException());

        //5.  verify vendor Game status with currency
        VendorGameCurrency vendorGameCurrency = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyIdAndStatus(
                rawGameSession.getVendorGameId(), rawGameSession.getCurrencyId(), Status.ACTIVE.code);
        Optional.ofNullable(vendorGameCurrency).orElseThrow(() -> new DisabledGameException());


    }
}
