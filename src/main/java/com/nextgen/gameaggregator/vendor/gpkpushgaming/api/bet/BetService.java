package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.bet;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.BetType;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.PlatformType;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class BetService {

    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final HttpService httpService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final VendorGameCodeService vendorGameCodeService;

    @Autowired
    public BetService(GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      WalletService walletService,
                      ValidationService validationService,
                      HttpService httpService,
                      SettledBetService settledBetService,
                      RequestIdempotentLogService requestIdempotentLogService,
                      AutowireCapableBeanFactory autowireCapableBeanFactory,
                      VendorGameCodeService vendorGameCodeService) {

        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.httpService = httpService;
        this.requestIdempotentLogService = requestIdempotentLogService;
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.vendorGameCodeService = vendorGameCodeService;
    }

    public CommonVo transaction(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo vo = new CommonVo();
        BetDto betDto = new BetDto();
        BetDataVo betDataVo = new BetDataVo();
        BigDecimal balance = null;
        GameSession gameSession = new GameSession();
        String gameCode = null;
        BigDecimal money = null;

        VendorService vendorService = new VendorService(vendorGameCodeService);
        autowireCapableBeanFactory.autowireBean(vendorService);

        try {
            betDto = HttpService.convertQueryStringToDto(URLDecoder.decode(httpRequestLog.getRequestBody(), StandardCharsets.UTF_8), BetDto.class);


            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // request Idempotent checking
            this.requestIdempotentChecking(betDto);

            gameCode = betDto.getGameinfo();

            // Verify session
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getUser());

            // update game code from session
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(gameCode, gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            //pushgaming
            if (betDto.getFinished() == null) {
                BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
                balance = betEvent.getLastBalance();
            } else if (betDto.getCode().equals(BetType.POINTOUT) && betDto.getFinished().equals(BetType.FINISHED)) {
                ResultType updatedResultType = getResultType(betDto);
                balance = walletService.processBetResult(traceId, gameSession, betDto, updatedResultType, vendorService, httpRequestLog);
            }

            vo.setCodeMsg(ResponseCodes.SUCCESS.code);

            // check the code value to define it is deducted or gain money
            money = betDto.getCode().equals(BetType.POINTIN) ? (betDto.getMoney().multiply(BigDecimal.valueOf(-1.00))) : betDto.getMoney();

            betDataVo.setDealid(betDto.getDealid());
            betDataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
            betDataVo.setMoney(money.setScale(2, RoundingMode.DOWN));
            if (balance != null) {
                betDataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());
            }
            vo.setData(betDataVo);
        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE.code);
        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.SUCCESS.code);

            try {
                balance = getCurrentBalance(traceId, gameSession, httpRequestLog);
            } catch (InvalidAgentApiCredentialException | VendorCurrencyNotSupportException |
                     InvalidOperatorResponseException ex) {
                httpService.logError(httpRequestLog, ex);
                vo.setCodeMsg(ResponseCodes.ERROR.code);
            }

            // check the code value to define it is deducted or gain money
            money = betDto.getCode().equals(BetType.POINTIN) ? (betDto.getMoney().multiply(BigDecimal.valueOf(-1.00))) : betDto.getMoney();

            betDataVo.setDealid(betDto.getDealid());
            betDataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
            betDataVo.setMoney(money.setScale(2, RoundingMode.DOWN));
            if (balance != null) {
                betDataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());
            }
            vo.setData(betDataVo);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR.code);
        }
        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

    }

    private void doVerification(BetDto dto, GameSession gameSession) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidRequestException, GameNotSupportedException {
        //validate vendor username, agent vendor line, player status, and game status
        if (dto.getCode().equals(BetType.POINTIN)) { //only check if it's bet
            validationService.validateEligibleBet(gameSession, dto.getUser());
        }

        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameinfo(), GameNotSupportedException::new);

        //Verify received api_token is same with credential
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_TOKEN);
        ValidationUtils.isEquals(token, dto.getApiToken(), InvalidRequestException::new);

        // check platform id
        if (!PlatformType.getPlatformTypeList().contains(dto.getPlatform())) {
            throw new InvalidRequestException();
        }
    }

    private ResultType getResultType(BetDto dto) {
        ResultType resultType = ResultType.WIN; // Default value is win

        if (isPushGamingPlatform(dto)) {
            if (isFinishedBet(dto)) {
                resultType = handleFinishedBet(dto);
            } else {
                resultType = handleUnfinishedBet(dto);
            }
        }

        return resultType;
    }

    private boolean isPushGamingPlatform(BetDto dto) {
        return dto.getPlatform().equals(PlatformType.PUSHGAMING) || dto.getPlatform().equals(PlatformType.PUSHGAMINGLATAM);
    }

    private boolean isFinishedBet(BetDto dto) {
        return dto.getFinished() != null && dto.getFinished().equals(BetType.FINISHED);
    }

    private ResultType handleFinishedBet(BetDto dto) {
        if (dto.getCode().equals(BetType.POINTIN)) {
            // If did not lose all money or exactly lose
            return (dto.getBetinfo().subtract(dto.getMoney())).compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
        } else {
            // It means exactly win
            return ResultType.BET_WIN;
        }
    }

    private ResultType handleUnfinishedBet(BetDto dto) {
        if (dto.getBetinfo().compareTo(BigDecimal.ZERO) > 0) {
            // First round of bonus game, bet amount (did not lose all money or exactly lose)
            return (dto.getBetinfo().subtract(dto.getMoney())).compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
        } else {
            // Middle of bonus game (betinfo value is zero, so just check for the money value)
            return dto.getMoney().compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
        }
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        return walletService.getBalance(traceId, gameSession, httpRequestLog);
    }

    private boolean requestIdempotentChecking(BetDto betDto) throws TransactionStillProcessingException {
        if (requestIdempotentLogService.checkExists(betDto, betDto.getUser()) == null) {
            requestIdempotentLogService.create(betDto, betDto.getUser());
            return false;
        } else {
            throw new TransactionStillProcessingException();
        }
    }
}
