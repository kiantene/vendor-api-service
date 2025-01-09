package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.bet;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
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
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BetService {

    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final HttpService httpService;
    private final RedissonService redissonService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final SettledBetService settledBetService;
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final VendorGameCodeService vendorGameCodeService;

    @Autowired
    public BetService(GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      WalletService walletService,
                      ValidationService validationService,
                      HttpService httpService,
                      RedissonService redissonService,
                      SettledBetService settledBetService,
                      RequestIdempotentLogService requestIdempotentLogService,
                      AutowireCapableBeanFactory autowireCapableBeanFactory,
                      VendorGameCodeService vendorGameCodeService) {

        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.httpService = httpService;
        this.redissonService = redissonService;
        this.settledBetService = settledBetService;
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

        ResultType resultType = null;

        String gameCode = null;

        BigDecimal money = null;

        boolean isRequestExists = false;

        RLock userLock = null;

        VendorService vendorService = new VendorService(vendorGameCodeService);
        autowireCapableBeanFactory.autowireBean(vendorService);

        try {
            betDto = HttpService.convertQueryStringToDto(URLDecoder.decode(httpRequestLog.getRequestBody(), StandardCharsets.UTF_8), BetDto.class);


            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // request Idempotent checking
            if (requestIdempotentLogService.checkExists(betDto, betDto.getUser()) == null) {
                requestIdempotentLogService.create(betDto, betDto.getUser());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            gameCode = betDto.getGameinfo();

            try {
                // Verify session
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getUser());
            } catch (AuthenticationException authenticationException) {
                gameSession = gameSessionService.generateNewSessionToken(betDto.getUser()); //generate new token
                gameSessionService.updateByVendorGameCode(gameSession, gameCode);
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // update game code from session
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(gameCode, gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession, vendorService);

            //pushgaming
            if (betDto.getPlatform().equals(PlatformType.PUSHGAMING) || betDto.getPlatform().equals(PlatformType.PUSHGAMINGLATAM)) {
                if (betDto.getFinished() == null) {
                    // unsettled
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
                    balance = betEvent.getLastBalance();
                } else {
                    // settled
                    if (betDto.getCode().equals(BetType.POINTOUT) && betDto.getFinished().equals(BetType.FINISHED)) {
                        resultType = getResultType(betDto);

                        balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                    }
                }
            }

            vo.setCodeMsg(ResponseCodes.SUCCESS);

            // check the code value to define it is deducted or gain money
            money = betDto.getCode().equals(BetType.POINTIN) ? (betDto.getMoney().multiply(BigDecimal.valueOf(-1.00))) : betDto.getMoney();

            betDataVo.setDealid(betDto.getDealid());
            betDataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
            betDataVo.setMoney(money.setScale(2, RoundingMode.DOWN));
            betDataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());

            vo.setData(betDataVo);
        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE);
        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);

            vo.setCodeMsg(ResponseCodes.SUCCESS);

            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // check the code value to define it is deducted or gain money
            money = betDto.getCode().equals(BetType.POINTIN) ? (betDto.getMoney().multiply(BigDecimal.valueOf(-1.00))) : betDto.getMoney();

            betDataVo.setDealid(betDto.getDealid());
            betDataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
            betDataVo.setMoney(money.setScale(2, RoundingMode.DOWN));
            betDataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());

            vo.setData(betDataVo);
        } catch (InvalidPlayerException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 DisabledVendorLineException |
                 CredentialNotFoundException |
                 InvalidRequestException |
                 GameNotSupportedException |
                 TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR);
        } catch (InvalidOperatorResponseException e) {
            httpService.logError(httpRequestLog, e);

            // for booming & spinominal to check insufficient balance
            if (e.getOperatorStatus() == 11) {
                vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE);
            } else {
                vo.setCodeMsg(ResponseCodes.ERROR);
            }

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR);
        } finally {
            // Release the lock if we acquired it and still hold it
            if (userLock != null && userLock.isHeldByCurrentThread()) {
                userLock.unlock();
            }
            if (!isRequestExists) {
                requestIdempotentLogService.delete(betDto, betDto.getUser());
            }
        }

        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

    }

    private void doVerification(BetDto dto, GameSession gameSession, VendorService vendorService) throws BetResultIdempotentViolationException, InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidRequestException, GameNotSupportedException {
        //validate vendor username, agent vendor line, player status, and game status
        if (dto.getCode().equals(BetType.POINTIN)) { //only check if it's bet
            validationService.validateEligibleBet(gameSession, dto.getUser());
        }

        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameinfo(), GameNotSupportedException::new);

        //Verify received api_token is same with credential
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.api_token);
        ValidationUtils.isEquals(token, dto.getApiToken(), InvalidRequestException::new);

        // check platform id
        if (!PlatformType.PlatformTypeList.contains(dto.getPlatform())) {
            throw new InvalidRequestException();
        }
    }

    private ResultType getResultType(BetDto dto) {
        ResultType resultType = ResultType.WIN; // Default value is win

        //pushgaming
        if (dto.getPlatform().equals(PlatformType.PUSHGAMING) || dto.getPlatform().equals(PlatformType.PUSHGAMINGLATAM)) {
            //one time settlement

            if (dto.getFinished() != null && dto.getFinished().equals(BetType.FINISHED)) {
                // if end-round(normal bet or end of bonus game)
                if (dto.getCode().equals(BetType.POINTIN)) {
                    // did not lose all money or exactly lose
                    resultType = (dto.getBetinfo().subtract(dto.getMoney())).compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
                } else {
                    // it means exactly win
                    resultType = ResultType.BET_WIN;
                }
            } else {
                // unfinished
                if (dto.getBetinfo().compareTo(BigDecimal.ZERO) > 0) {
                    // first round of bonus game will happen bet amount (did not lose all money or exactly lose)
                    resultType = (dto.getBetinfo().subtract(dto.getMoney())).compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
                } else {
                    // middle of bonus game (betinfo value is zero,so just check for the money value)
                    resultType = dto.getMoney().compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
                }
            }
        }

        return resultType;
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) {

        BigDecimal balance = BigDecimal.ZERO;

        try {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

        } catch (Exception exception) {

        }

        return balance;
    }

    private void verifySettledBet(BetDto dto, GameSession gameSession) throws BetResultIdempotentViolationException {
        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), dto.getRoundId());

        if (!settledBetList.isEmpty() && settledBetList.get(0).getOperatorStatus().equals(1)) {
            throw new BetResultIdempotentViolationException(settledBetList.get(0));
        }
    }
}
