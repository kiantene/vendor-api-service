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

            // set betid to refer previous bet id when handle settled request for 7mojo
            if (betDto.getPlatform().equals(PlatformType.SEVENMOJO) || betDto.getPlatform().equals(PlatformType.SEVENMOJOLATAM)) {
                if (betDto.getCode().equals(BetType.POINTOUT) && betDto.getFinished().equals(BetType.FINISHED) && betDto.getIstips().equals(BetType.NOTTIPS)) {
                    betDto.setBetId(httpRequestLog.getRequestBody());
                }
            }

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
            // verify bet game code of 7mojo
            if (betDto.getPlatform().equals(PlatformType.SEVENMOJO) || betDto.getPlatform().equals(PlatformType.SEVENMOJOLATAM)) {
                // check credential env is available or not
                String runEnv = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.env);

                // if stg env
                if (runEnv.equalsIgnoreCase("stg")) {
                    gameCode += "_stg";
                }

                // check the game is existing in db or not
                VendorGameCode vendorGameCode = vendorService.getVendorGameCode(gameSession, gameCode);

                // re-assign value again(vendor's request only have bet game code)
                gameCode = vendorGameCode.getOpenGameCode();
            }

            // update game code from session
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(gameCode, gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession, vendorService);

            //7mojo
            if (betDto.getPlatform().equals(PlatformType.SEVENMOJO) || betDto.getPlatform().equals(PlatformType.SEVENMOJOLATAM)) {
                // 7Mojo Live game multiple bet will settle by bet
                if (gameSession.getGameCategoryId().equals(5)) {
                    vendorService.setSettledByBet(true);
                }

                if (betDto.getIstips().equals(BetType.TIPS)) {
                    // tips
                    balance = walletService.processBetResult(traceId, gameSession, betDto, ResultType.BET_LOSE, vendorService, httpRequestLog);
                } else {
                    // normal bet

                    if (betDto.getFinished().equals(BetType.UNFINISHED)) {
                        // unsettled

                        BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
                        balance = betEvent.getLastBalance();
                    } else {
                        //settled

                        resultType = getResultType(betDto);
                        balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                    }
                }
            }

            //turbo game
            if (betDto.getPlatform().equals(PlatformType.TURBOGAME) || betDto.getPlatform().equals(PlatformType.TURBOGAMELATAM)) {
                if (betDto.getDealid().contains("place") && betDto.getFinished() == null) {
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

            //bgaming
            if (betDto.getPlatform().equals(PlatformType.BGAMINGASIA) || betDto.getPlatform().equals(PlatformType.BGAMINGLATAM)) {
                if (betDto.getFinished().equals(BetType.FINISHED)) {
                    // if end-round

                    if (betDto.getCode().equals(BetType.POINTIN)) {
                        // slot game place bet and lose
                        balance = walletService.processBetResult(traceId, gameSession, betDto, ResultType.BET_LOSE, vendorService, httpRequestLog);
                    } else {
//                        Thread.sleep(150); // sleep 150ms avoid win endpoint haven't lock

                        userLock = redissonService.getRedissonClient().getLock("RedissonLock:GPK-BGMING:" + betDto.getRoundId());

                        // get the lock time
                        long remainTime = userLock.remainTimeToLive();

                        // if remainTime is -1 meant forever lock, -2 meant the lock is expired or not exist
                        while (remainTime != -2) {
                            remainTime = userLock.remainTimeToLive();

                            // if remainTime is forever lock then break the loop
                            if (remainTime == -1) {
                                break;
                            }
                        }

                        // settle bet
                        resultType = getResultType(betDto);

                        balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                    }
                } else {
                    // not yet end(unsettled)
                    if (betDto.getCode().equals(BetType.POINTIN)) {
                        // place bet
                        BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);

                        balance = betEvent.getLastBalance();
                    } else {
                        userLock = redissonService.getRedissonClient().getLock("RedissonLock:GPK-BGMING:" + betDto.getRoundId());
                        userLock.lock(5, TimeUnit.SECONDS);

                        // mini game un-finished win request
                        resultType = getResultType(betDto);

                        balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                    }
                }
            }

            //booming & spinomenal (these 2 platforms are process as one time settlement no matter it is normal bet or many rounds of bonus game)
            if (betDto.getPlatform().equals(PlatformType.BOOMING) || betDto.getPlatform().equals(PlatformType.BOOMINGLATAM) || betDto.getPlatform().equals(PlatformType.SPINOMENAL) || betDto.getPlatform().equals(PlatformType.SPINOMENALLATAM)) {
                // one time settlement(normal bet) and bonus game

                resultType = getResultType(betDto);
                balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
            }

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

        // if 7mojo platform will check istips param
        if (dto.getPlatform().equals(PlatformType.SEVENMOJO) || dto.getPlatform().equals(PlatformType.SEVENMOJOLATAM)) {
            Optional.ofNullable(dto.getIstips()).orElseThrow(InvalidRequestException::new);

            // check finished param when it is not tips
            if (dto.getIstips().equals(BetType.NOTTIPS)) {
                Optional.ofNullable(dto.getFinished()).orElseThrow(InvalidRequestException::new);
            }

            Optional.ofNullable(dto.getDealid()).orElseThrow(InvalidRequestException::new);

            // make sure it is settled request
            if (dto.getIstips().equals(BetType.NOTTIPS) && dto.getFinished().equals(BetType.FINISHED) && dto.getCode().equals(BetType.POINTOUT)) {
                Optional.ofNullable(dto.getBetid()).orElseThrow(InvalidRequestException::new);
            }
        }

        // if turbo game platform will check finished param when end-round
        if (dto.getPlatform().equals(PlatformType.TURBOGAME) || dto.getPlatform().equals(PlatformType.TURBOGAMELATAM)) {
            Optional.ofNullable(dto.getDealid()).orElseThrow(InvalidRequestException::new);

            if (dto.getCode().equals(BetType.POINTOUT) && dto.getDealid().contains("settle")) {
                Optional.ofNullable(dto.getFinished()).orElseThrow(InvalidRequestException::new);
            }
        }

        // if bgaming platform will check finished param
        if (dto.getPlatform().equals(PlatformType.BGAMINGASIA) || dto.getPlatform().equals(PlatformType.BGAMINGLATAM)) {
            Optional.ofNullable(dto.getFinished()).orElseThrow(InvalidRequestException::new);

            // check the dealid if it is not lose game in buy finish game
            if (!(dto.getMoney().compareTo(BigDecimal.ZERO) == 0 && dto.getCode().equals(BetType.POINTOUT) && dto.getFinished().equals(BetType.FINISHED))) {
                Optional.ofNullable(dto.getDealid()).orElseThrow(InvalidRequestException::new);
            }
        }

        // if booming platform will check root_dealid, root_roundid & betinfo
        if (dto.getPlatform().equals(PlatformType.BOOMING) || dto.getPlatform().equals(PlatformType.BOOMINGLATAM)) {
            Optional.ofNullable(dto.getRootDealid()).orElseThrow(InvalidRequestException::new);
            Optional.ofNullable(dto.getRootDealid()).map(String::isEmpty).orElseThrow(InvalidRequestException::new);
            Optional.ofNullable(dto.getRootRoundid()).orElseThrow(InvalidRequestException::new);
            Optional.ofNullable(dto.getRootRoundid()).map(String::isEmpty).orElseThrow(InvalidRequestException::new);
            Optional.ofNullable(dto.getBetinfo()).orElseThrow(InvalidRequestException::new);
        }
    }

    private void doVerification(BetDto dto, GameSession gameSession, VendorService vendorService) throws BetResultIdempotentViolationException, InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidRequestException, GameNotSupportedException {
        //validate vendor username, agent vendor line, player status, and game status
        if (dto.getCode().equals(BetType.POINTIN)) { //only check if it's bet
            validationService.validateEligibleBet(gameSession, dto.getUser());
        }
        // Verify vendor gameCode
        List<String> sevenmojoPlatform = Arrays.asList(PlatformType.SEVENMOJO, PlatformType.SEVENMOJOLATAM);

        // BGAMINGASIA and BGAMINGLATAM will check idempotent by player and round id before bet
        if ((dto.getPlatform().equals(PlatformType.BGAMINGLATAM))
                || (dto.getPlatform().equals(PlatformType.BGAMINGASIA))) {
            this.verifySettledBet(dto, gameSession);
        }

        // check platform (only 7mojo will return bet game code)
        if (!sevenmojoPlatform.contains(dto.getPlatform())) {
            ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameinfo(), GameNotSupportedException::new);
        } else {
            // check credential env is available or not
            String runEnv = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.env);

            String gameCode = dto.getGameId();

            if (runEnv.equalsIgnoreCase("stg")) {
                gameCode += "_stg";
            }

            vendorService.verifyVendorGameCode(gameSession, gameCode);
        }

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

        //7mojo & turbo game
        if (dto.getPlatform().equals(PlatformType.SEVENMOJO) || dto.getPlatform().equals(PlatformType.SEVENMOJOLATAM) || dto.getPlatform().equals(PlatformType.TURBOGAME) || dto.getPlatform().equals(PlatformType.TURBOGAMELATAM)) {
            if (dto.getMoney().compareTo(BigDecimal.ZERO) == 0 && dto.getCode().equals(BetType.POINTOUT)) {
                resultType = ResultType.END;
            }
        }

        //bgaming
        if (dto.getPlatform().equals(PlatformType.BGAMINGASIA) || dto.getPlatform().equals(PlatformType.BGAMINGLATAM)) {
            // does not handle mini game un-finished settled request coz it is win status
            // does not handle slot game normal lose coz it is one time settlement

            if (dto.getFinished().equals(BetType.FINISHED)) {
                if (dto.getMoney().compareTo(BigDecimal.ZERO) == 0 && dto.getDealid() == null) {
                    resultType = ResultType.END;
                }
            }
        }

        //booming
        if (dto.getPlatform().equals(PlatformType.BOOMING) || dto.getPlatform().equals(PlatformType.BOOMINGLATAM)) {
            // one time settlement

            if (dto.getFinished().equals(BetType.FINISHED)) {
                // settled
                if (dto.getBRoundid().equals(dto.getRootRoundid()) && dto.getDealid().equals(dto.getRootDealid())) {
                    //one time settlement
                    if (dto.getCode().equals(BetType.POINTIN)) {
                        // did not lose all money or exactly lose
                        resultType = (dto.getBetinfo().subtract(dto.getMoney())).compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
                    } else {
                        // it means exactly win
                        resultType = ResultType.BET_WIN;
                    }
                } else {
                    //end of bonus game
                    //only use pointout(no matter win or lose)
                    //no bet amount so just use money to define win or lose
                    resultType = dto.getMoney().compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
                }
            } else {
                // unsettled
                if (dto.getBRoundid().equals(dto.getRootRoundid()) && dto.getDealid().equals(dto.getRootDealid())) {
                    //start of bonus game

                    if (dto.getCode().equals(BetType.POINTIN)) {
                        // did not lose all money or exactly lose
                        resultType = (dto.getBetinfo().subtract(dto.getMoney())).compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
                    } else {
                        // it means exactly win
                        resultType = ResultType.BET_WIN;
                    }
                } else {
                    //middle of bonus game
                    if (dto.getCode().equals(BetType.POINTOUT)) {
                        //only use pointout(no matter win or lose)
                        //no bet amount so just use money to define win or lose
                        resultType = dto.getMoney().compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
                    }
                }
            }
        }

        //spinomenal
        if (dto.getPlatform().equals(PlatformType.SPINOMENAL) || dto.getPlatform().equals(PlatformType.SPINOMENALLATAM)) {
            //one time settlement

            if (dto.getFinished().equals(BetType.FINISHED)) {
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
