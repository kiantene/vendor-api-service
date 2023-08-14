package com.nextgen.gameaggregator.vendor.bgaming.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.bgaming.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class BalanceService {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    public ResponseVo balance(CommonDto commonDto, HttpRequestLog httpRequestLog, HttpServletRequest request) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, InvalidRequestException, DisabledVendorLineException, InvalidAgentApiCredentialException, InvalidOperatorResponseException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException {
        String traceId = httpRequestLog.getId();
        ResponseVo responseVo = new ResponseVo();

        // Get vendor player details
        GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(commonDto.getUserId());

        // Verify remaining parameters (Verify against database values)
        this.doVerification(commonDto, gameSession, httpRequestLog, request);

        // Get walletBalance
        BigDecimal balance = walletService.getBalance(traceId, gameSession);

        // Convert Amount
        Integer convertedBalance = vendorService.convertAmountToInteger(balance, commonDto.getCurrency());

        // Construct VO
        responseVo.setBalance(convertedBalance);

        return responseVo;
    }

    private void doVerification(CommonDto commonDto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request) throws
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, CredentialNotFoundException, InvalidSignatureException, JsonProcessingException, CurrencyNotSupportedException {
        // Verify Status
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);

        // Convert Body to Map for signature check
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> bodyObj = mapper.readValue(httpRequestLog.getRequestBody(), Map.class);

        // Verify Signature key from vendor given
        String authToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AUTH_TOKEN);
        VendorService.verifySign(authToken, new Gson().toJson(bodyObj), request.getHeader("X-REQUEST-SIGN"));
    }
}
