package com.nextgen.gameaggregator.vendor.bgaming.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
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
public class EndRoundService {
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;

    public void endRound(CommonDto commonDto, ActionDto actionDto, HttpServletRequest request, ResponseVo responseVo, GameSession gameSession) throws AuthenticationException, DisabledAgentPlayerException, DisabledGameException, InvalidRequestException, DisabledVendorLineException, InvalidAgentApiCredentialException, BetResultIdempotentViolationException, MergedBetDataIntegrityException, InsufficientBalanceException, TransactionStillProcessingException, BetNotFoundException, InvalidOperatorResponseException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException, VendorCurrencyNotSupportException {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        TransactionVo transactionVo = new TransactionVo();

        try {
            EndRoundDto endRoundDto = new ModelMapper().map(commonDto, EndRoundDto.class);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, gameSession, httpRequestLog, request);

            // Prepare Data for process bet
            ResultType resultType = ResultType.END;

            if (actionDto != null) {
                if (actionDto.getAction().equals("win")) {
                    resultType = ResultType.WIN;
                }
            }

            //testing
            Thread.sleep(10000);

            // process settled bet
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, endRoundDto, resultType, vendorService, httpRequestLog);

            // Construct VO - if win then add to transaction
            if (actionDto != null && actionDto.getAction().equals("win")) {
                transactionVo.setActionId(actionDto.getActionId());
                transactionVo.setTxId(actionDto.getActionId());
                transactionVo.setProcessedAt(new DateTime(endRoundDto.getVendorSettleTime()).toString());
                responseVo.addTransactions(transactionVo);
            }

            responseVo.setBalance(balance.intValue());
            responseVo.setGameId(commonDto.getVendorRoundId());

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
    }

    private void doVerification(CommonDto commonDto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request) throws
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException {
        // Verify Status
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);
    }
}