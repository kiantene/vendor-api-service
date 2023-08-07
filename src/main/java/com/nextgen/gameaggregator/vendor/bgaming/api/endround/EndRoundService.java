package com.nextgen.gameaggregator.vendor.bgaming.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.vo.TransactionVo;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
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
public class EndRoundService {
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
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    public TransactionVo endRound(CommonDto commonDto, ActionDto actionDto, HttpRequestLog httpRequestLog, HttpServletRequest request) throws AuthenticationException, DisabledAgentPlayerException, DisabledGameException, InvalidRequestException, DisabledVendorLineException, InvalidAgentApiCredentialException, BetResultIdempotentViolationException, MergedBetDataIntegrityException, InsufficientBalanceException, TransactionStillProcessingException, BetNotFoundException, InvalidOperatorResponseException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException, GameNotSupportedException {
        String traceId = httpRequestLog.getId();
        TransactionVo transactionVo = new TransactionVo();

        EndRoundDto endRoundDto = new ModelMapper().map(commonDto, EndRoundDto.class);

        // Get vendor player details
        GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(commonDto.getUserId());

        // Verify remaining parameters (Verify against database values)
        this.doVerification(commonDto, gameSession, httpRequestLog, request);

        // Prepare Data for process bet
        ResultType resultType = ResultType.END;
        if (actionDto == null) {
            endRoundDto.setBetId(commonDto.getGameId());
        } else {
            endRoundDto.setBetId(actionDto.getActionId());
            if (actionDto.getAction().equals("win")) {
                resultType = ResultType.WIN;
                BigDecimal amount = vendorService.convertAmountToBigDecimal(actionDto.getAmount(), commonDto.getCurrency());
                endRoundDto.setWinAmount(amount);
            }
        }
        endRoundDto.setTimestamp(httpRequestLog.getStartTime());

        // process settled bet
        BigDecimal balance = walletService.processBetResult(traceId, gameSession, endRoundDto, resultType, vendorService, httpRequestLog);

        // Convert Amount
        Integer convertedBalance = vendorService.convertAmountToInteger(balance, commonDto.getCurrency());

        // Construct VO
        transactionVo.setBalance(convertedBalance);
        if (actionDto != null) {
            transactionVo.setActionId(actionDto.getActionId());
            transactionVo.setTxId(actionDto.getActionId());
            transactionVo.setProcessedAt(new DateTime(endRoundDto.getVendorSettleTime()).toString());
        }

        return transactionVo;
    }

    private void doVerification(CommonDto commonDto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request) throws
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException {
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
