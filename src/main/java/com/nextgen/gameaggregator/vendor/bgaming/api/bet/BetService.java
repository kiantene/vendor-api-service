package com.nextgen.gameaggregator.vendor.bgaming.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.api.endround.EndRoundService;
import com.nextgen.gameaggregator.vendor.bgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.bgaming.vo.TransactionVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class BetService {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private EndRoundService endRoundService;
    @Autowired
    private VendorLineService vendorLineService;

    public TransactionVo bet(CommonDto commonDto, ActionDto actionDto, HttpRequestLog httpRequestLog, HttpRequestLog parentHttpRequestLog, HttpServletRequest request) throws AuthenticationException, InvalidPlayerException, DisabledAgentPlayerException, DisabledGameException, InvalidRequestException, DisabledVendorLineException, InvalidAgentApiCredentialException, BetResultIdempotentViolationException, InsufficientBalanceException, TransactionStillProcessingException, InvalidOperatorResponseException, CouchbaseDataIntegrityException, MergedBetDataIntegrityException, BetNotFoundException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException, GameNotSupportedException, VendorCurrencyNotSupportException {
        String traceId = httpRequestLog.getId();
        TransactionVo transactionVo = new TransactionVo();

        BetDto betDto = new ModelMapper().map(commonDto, BetDto.class);

        //Retrieve request body in original string format
        String body = httpRequestLog.getRequestBody();

        // Get vendor player details
        GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(commonDto.getUserId());

        // Verify remaining parameters (Verify against database values)
        this.doVerification(commonDto, actionDto, gameSession, parentHttpRequestLog, request);

        // Prepare Data for process bet
        betDto.setBetId(actionDto.getActionId());
        BigDecimal amount = vendorService.convertAmountToBigDecimal(actionDto.getAmount(), commonDto.getCurrency());
        betDto.setBetAmount(amount);
        betDto.setTimestamp(System.currentTimeMillis());

        // Process unsettled bet
        BigDecimal balance = null;
        BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);
        balance = betEvent.getLastBalance();

        // Convert Amount
        Integer convertedBalance = vendorService.convertAmountToInteger(balance, commonDto.getCurrency());

        // Construct VO
        transactionVo.setBalance(convertedBalance);
        transactionVo.setActionId(actionDto.getActionId());
        transactionVo.setTxId(actionDto.getActionId());
        transactionVo.setProcessedAt(new DateTime(betDto.getVendorBetTime()).toString());

        return transactionVo;
    }

    private void doVerification(CommonDto commonDto, ActionDto actionDto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request) throws InvalidPlayerException,
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, AuthenticationException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException, BetResultIdempotentViolationException {
        // Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, commonDto.getUserId());

        // Verify status
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);

        // Convert Body to Map for signature check
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> bodyObj = mapper.readValue(httpRequestLog.getRequestBody(), Map.class);

        // Verify Signature key from vendor given
        String authToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AUTH_TOKEN);
        VendorService.verifySign(authToken, new Gson().toJson(bodyObj), request.getHeader("X-REQUEST-SIGN"));

        // Check round is settled or not
        vendorService.verifySettledRound(gameSession.getVendorPlayerId(), commonDto.getVendorRoundId());
    }
}
