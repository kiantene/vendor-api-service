package com.nextgen.gameaggregator.vendor.bgaming.api.bet;

import com.couchbase.client.core.deps.com.google.gson.Gson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.bgaming.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.bgaming.vo.TransactionVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BetService {
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;

    public void bet(CommonDto commonDto, HttpServletRequest request, ResponseVo responseVo, GameSession gameSession) throws AuthenticationException, InvalidPlayerException, DisabledAgentPlayerException, DisabledGameException, InvalidRequestException, DisabledVendorLineException, InvalidAgentApiCredentialException, BetResultIdempotentViolationException, InsufficientBalanceException, TransactionStillProcessingException, InvalidOperatorResponseException, CouchbaseDataIntegrityException, MergedBetDataIntegrityException, BetNotFoundException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException, GameNotSupportedException, VendorCurrencyNotSupportException {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        TransactionVo transactionVo = new TransactionVo();

        try {
            BetDto betDto = new ModelMapper().map(commonDto, BetDto.class);

            //Insert Request Body
            Gson gson = new Gson();
            httpRequestLog.setRequestBody(gson.toJson(commonDto));

            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, gameSession);

            // Process unsettled bet
            BigDecimal balance = null;
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body, httpRequestLog);
            balance = betEvent.getLastBalance();

            // Construct VO
            transactionVo.setActionId(commonDto.getActionDto().getActionId());
            transactionVo.setTxId(commonDto.getActionDto().getActionId());
            transactionVo.setProcessedAt(new DateTime(betDto.getVendorBetTime()).toString());

            responseVo.addTransactions(transactionVo);
            responseVo.setBalance(balance.intValue());
            responseVo.setGameId(commonDto.getVendorRoundId());

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
    }

    private void doVerification(CommonDto commonDto, GameSession gameSession) throws InvalidPlayerException,
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, AuthenticationException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException, BetResultIdempotentViolationException {
        // Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, commonDto.getUserId());

        // Verify status
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);

        // Check round is settled or not
        vendorService.verifySettledRound(gameSession.getVendorPlayerId(), commonDto.getVendorRoundId());
    }
}