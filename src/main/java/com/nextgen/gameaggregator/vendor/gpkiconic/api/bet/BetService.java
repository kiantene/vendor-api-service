package com.nextgen.gameaggregator.vendor.gpkiconic.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkiconic.constant.BetType;
import com.nextgen.gameaggregator.vendor.gpkiconic.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkiconic.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkiconic.service.VendorService;
import com.nextgen.gameaggregator.vendor.gpkiconic.vo.CommonVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class BetService {
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final HttpService httpService;
    private final VendorService vendorService;

    public BetService(GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      WalletService walletService,
                      ValidationService validationService,
                      HttpService httpService,
                      VendorService vendorService) {

        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.httpService = httpService;
        this.vendorService = vendorService;
    }

    public CommonVo transaction(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo vo = new CommonVo();
        BetDto betDto = new BetDto();
        BetDataVo betDataVo = new BetDataVo();
        BigDecimal balance = BigDecimal.ZERO;
        GameSession gameSession = new GameSession();
        BigDecimal money;

        try {
            betDto = HttpService.convertQueryStringToDto(URLDecoder.decode(httpRequestLog.getRequestBody(),
                            StandardCharsets.UTF_8),
                    BetDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // Verify session
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getUser());

            // update game code from session
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betDto.getGameInfo(),
                    gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto,
                    gameSession);

            boolean isIconicLive = gameSession.getGameCategoryId().equals(5);
            boolean isTips = BetType.isTips(betDto.getIsTips());

            if (isIconicLive && isTips) {
                // tips
                balance = walletService.processBetResult(traceId,
                        gameSession,
                        betDto,
                        ResultType.BET_LOSE,
                        vendorService,
                        httpRequestLog);
            } else {
                balance = this.processBetOrSettlement(traceId,
                        gameSession,
                        betDto,
                        httpRequestLog);
            }

            vo.setCodeMsg(ResponseCodes.SUCCESS.code);

            // check the code value to define it is deducted or gain money
            money = betDto.getCode().equals(BetType.POINTIN) ? (betDto.getMoney().negate()) : betDto.getMoney();

            betDataVo.setDealid(betDto.getDealid());
            betDataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
            betDataVo.setMoney(money.setScale(2,
                    RoundingMode.DOWN));
            if (balance != null) {
                betDataVo.setCash(balance.setScale(2,
                        RoundingMode.DOWN).toString());
            }
            vo.setData(betDataVo);
        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog,
                    e);
            vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE.code);
        } catch (BetNotFoundException | BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog,
                    e);
            vo.setCodeMsg(ResponseCodes.SUCCESS.code);

            try {
                balance = vendorService.getCurrentBalance(traceId,
                        gameSession,
                        httpRequestLog);
            } catch (InvalidAgentApiCredentialException | VendorCurrencyNotSupportException |
                     InvalidOperatorResponseException ex) {
                httpService.logError(httpRequestLog,
                        ex);
                vo.setCodeMsg(ResponseCodes.ERROR.code);
            }

            // check the code value to define it is deducted or gain money
            money = betDto.getCode().equals(BetType.POINTIN) ?
                    (betDto.getMoney().negate()) : betDto.getMoney();

            betDataVo.setDealid(betDto.getDealid());
            betDataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
            betDataVo.setMoney(money.setScale(2,
                    RoundingMode.DOWN));
            if (balance != null) {
                betDataVo.setCash(balance.setScale(2,
                        RoundingMode.DOWN).toString());
            }
            vo.setData(betDataVo);
        } catch (Exception e) {
            httpService.logError(httpRequestLog,
                    e);
            vo.setCodeMsg(ResponseCodes.ERROR.code);
        }
        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetDto dto, GameSession gameSession) throws InvalidPlayerException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException,
            CredentialNotFoundException,
            InvalidRequestException,
            GameNotSupportedException {
        //validate vendor username, agent vendor line, player status, and game status
        if (dto.getCode().equals(BetType.POINTIN)) { //only check if it's bet
            validationService.validateEligibleBet(gameSession,
                    dto.getUser());
        }

        ValidationUtils.isEquals(gameSession.getVendorGameCode(),
                dto.getGameInfo(),
                GameNotSupportedException::new);

        //Verify received api_token is same with credential
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(),
                Credentials.API_TOKEN);
        ValidationUtils.isEquals(token,
                dto.getApiToken(),
                InvalidRequestException::new);

        // check provider id
        String provider = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(),
                Credentials.PROVIDER_ID);
        ValidationUtils.isEquals(provider,
                dto.getProvider(),
                InvalidRequestException::new);
    }

    private BigDecimal processBetOrSettlement(String traceId, GameSession gameSession, BetDto betDto,
                                              HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException,
            VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException,
            InsufficientBalanceException,
            TransactionStillProcessingException,
            InvalidOperatorResponseException,
            CouchbaseDataIntegrityException,
            MergedBetDataIntegrityException,
            BetNotFoundException,
            InternalServerTimeoutRetryException {

        BigDecimal balance;
        // bet
        if (betDto.getCode().equals(BetType.POINTIN)) {
            // unsettled
            BetEvent betEvent = walletService.processBet(traceId,
                    gameSession,
                    betDto,
                    httpRequestLog.getRequestBody(),
                    httpRequestLog);
            balance = betEvent.getLastBalance();

        } else {
            // settled
            ResultType resultType = vendorService.getResultType(betDto);
            balance = walletService.processBetResult(traceId,
                    gameSession,
                    betDto,
                    resultType,
                    vendorService,
                    httpRequestLog);
        }
        return balance;
    }
}

