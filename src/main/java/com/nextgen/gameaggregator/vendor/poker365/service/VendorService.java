package com.nextgen.gameaggregator.vendor.poker365.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.constant.Credentials;
import com.nextgen.gameaggregator.vendor.poker365.dto.CommonDto;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class VendorService extends BaseVendorService {


    private final ValidationService validationService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;

    private VendorService(ValidationService validationService,
                          VendorLineService vendorLineService,
                          AgentPlayerService agentPlayerService)
    {
        this.validationService = validationService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        //Temporary only BGAMING, SpadeGaming, EvoNetent need to accept cancel request
        return false;
    }


    public void doVerification(CommonDto commonDto, GameSession gameSession, String userId, String currency, String gameId)
            throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            CredentialNotFoundException,
            InvalidVendorLineException,
            InvalidPlayerException,
            DisabledGameException,
            CurrencyNotSupportedException,
            GameNotSupportedException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String cert = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CERT);
        ValidationUtils.isEquals(cert, commonDto.getKey(), AuthenticationException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerId()), String.valueOf(userId), InvalidPlayerException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), currency, CurrencyNotSupportedException::new);

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), gameId, GameNotSupportedException::new);
    }
}
