package com.nextgen.gameaggregator.vendor.bglive.api.settlement;


import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralCreditDto;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralSettleDto;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.GameCode;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.constant.ThreadSize;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bglive.vo.ResultVo;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class SettlementService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final BetActionLogService betActionLogService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final OperatorWalletService operatorWalletService;
    private final WalletTransactionBetHistoryService walletTransactionBetHistoryService;
    private final UnsettledBetCachingService unsettledBetCachingService;
    private final WalletTransactionService walletTransactionService;


    public SettlementService(HttpService httpService,
                             WalletService walletService,
                             GameSessionService gameSessionService,
                             VendorLineService vendorLineService,
                             AgentPlayerService agentPlayerService,
                             VendorService vendorService,
                             BetActionLogService betActionLogService,
                             RequestIdempotentLogService requestIdempotentLogService,
                             OperatorWalletService operatorWalletService,
                             WalletTransactionBetHistoryService walletTransactionBetHistoryService, UnsettledBetCachingService unsettledBetCachingService, WalletTransactionService walletTransactionService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
        this.betActionLogService = betActionLogService;
        this.requestIdempotentLogService = requestIdempotentLogService;
        this.operatorWalletService = operatorWalletService;
        this.walletTransactionBetHistoryService = walletTransactionBetHistoryService;
        this.unsettledBetCachingService = unsettledBetCachingService;
        this.walletTransactionService = walletTransactionService;
    }

    public CommonVo settle(HttpRequestLog httpRequestLog, HttpServletRequest request) {
        CommonVo commonVo = new CommonVo();
        ExecutorService executor = null;
        try {
//            Thread.sleep(10000);
            String body = httpRequestLog.getRequestBody();
            SettleDto settleDto = HttpService.convertJsonToDto(body, SettleDto.class);
            int orderCount = settleDto.getParamsDto().getOrders().size();
            executor = vendorService.createThreadPool(orderCount);
            this.doValidation(settleDto);
            GameSession gameSession = this.getGameSession(settleDto.getParamsDto().getLoginId());
            this.doVerification(settleDto, gameSession);

            List<CompletableFuture<ResultVo>> balanceList = this.processAllOrders(settleDto, request, gameSession,
                    executor);
            BigDecimal balance = vendorService.checkSettleResponseAndReturnBalance(balanceList);
            if (balance == null) {
                throw new BetResultNotFoundException("Have Transaction Failed");
            }
            ResultVo resultVo = this.createSuccessResultVo(gameSession, settleDto, balance);
            commonVo.setSuccessResponse(settleDto.getId(), resultVo);

        } catch (Exception e) {
            this.handleException(e, commonVo, httpRequestLog);
        } finally {
            // close executor
            if (executor != null) {
                executor.shutdown();
            }

        }
        return commonVo;
    }

    //process all orders
    private List<CompletableFuture<ResultVo>> processAllOrders(SettleDto settleDto, HttpServletRequest httpServletRequest,
                                                               GameSession gameSession, ExecutorService executor) {
        List<CompletableFuture<ResultVo>> balanceList = new LinkedList<>();
        for (OrdersDto order : settleDto.getParamsDto().getOrders()) {
            CompletableFuture<ResultVo> balance = CompletableFuture.supplyAsync(() -> this.processData(settleDto.getParamsDto(),
                    order, httpServletRequest, gameSession), executor);
            balanceList.add(balance);
        }
        return balanceList;
    }

    private void doValidation(SettleDto settleDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(settleDto);

        ParamsDto paramsDto = settleDto.getParamsDto();
        if (paramsDto != null) {
            ValidationUtils.validateRequest(paramsDto);

            List<OrdersDto> ordersList = paramsDto.getOrders();
            if (ordersList == null || ordersList.isEmpty()) {
                throw new InvalidRequestException("Settle request must contain at least one order.");
            }
            for (OrdersDto order : ordersList) {
                ValidationUtils.validateRequest(order);
            }
        }
    }

    private void doValidation(OrdersDto ordersDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(ordersDto);
    }

    private void doVerification(SettleDto settleDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidFormatException {

        String snCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SN_CODE);
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(snCode, settleDto.getParamsDto().getSn(), InvalidPlayerException::new);

        String validateSign = VendorService.encryptBetMd5Key(settleDto.getParamsDto().getRandom(), snCode,
                gameSession.getVendorPlayerUsername(), String.valueOf(settleDto.getParamsDto().getAmount()), secretKey);
        ValidationUtils.isEquals(validateSign, settleDto.getParamsDto().getSign(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }

    private GameSession getGameSession(String loginId) throws AuthenticationException {
        return gameSessionService.getGameSessionByVendorPlayerUsername(loginId);
    }

    //Concurrent process orders
    private ResultVo processData(ParamsDto paramsDto, OrdersDto ordersDto, HttpServletRequest httpServletRequest,
                                 GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(httpServletRequest);
        httpRequestLog.setRequestBody(new Gson().toJson(ordersDto));
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        WalletTransaction walletTransaction;
        String traceId = httpRequestLog.getId();
        BigDecimal balance;
        boolean isRequestExists = false;
        ResultType resultType = null;
        ResultVo resultVo;
        try {
            this.doValidation(ordersDto);

            //regenerate token
            UnsettledBet unsettledBet = unsettledBetCachingService.getTop1UnsettledBetWithRoundId(ordersDto.getOrderId());
            walletTransaction = walletTransactionService.getByVendorIdAndExternalTransactionId(gameSession.getVendorId(), ordersDto.getExternalTransactionId());
            if (unsettledBet == null && walletTransaction == null) {
                throw new BetNotFoundException("Cannot find round Id: " + ordersDto.getRoundId());
            } else {
                gameSession = getVendorPlayerId(unsettledBet, walletTransaction);

                gameSessionService.updateByVendorGameCode(gameSession, gameSession.getVendorGameCode());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }
            // Request idempotent checking for this transaction
            if (requestIdempotentLogService.checkExists(ordersDto, paramsDto.getLoginId()) == null) {
                requestIdempotentLogService.create(ordersDto, paramsDto.getLoginId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            boolean isBullBullGame = ordersDto.getGameId().equals(GameCode.BULL_BULL);
            if (isBullBullGame) {
                WalletRequest currentWalletRequest = new WalletRequest(walletRequest);
                vendorService.dataCreditMapper(currentWalletRequest, ordersDto, gameSession);
                walletRequest = operatorWalletService.betCredit(currentWalletRequest);
                //Update walletTransaction Bet History
                walletTransactionBetHistoryService.update(currentWalletRequest);
                resultVo = new ResultVo(walletRequest.getBalanceAfter(), httpRequestLog.getOperatorTimestamp());
            } else {
                // Process Result
                resultType = vendorService.calculateResultType(ordersDto.getBetAmount(), ordersDto.getAmount().abs(),
                        ordersDto.getJackpotAmount(), false);
                balance = walletService.processBetResult(traceId, gameSession, ordersDto, resultType, vendorService,
                        httpRequestLog);
                resultVo = new ResultVo(balance, httpRequestLog.getOperatorTimestamp());
            }

        } catch (InsufficientBalanceException |
                 InvalidRequestException |
                 TransactionStillProcessingException e) {
            resultVo = new ResultVo(BigDecimal.ZERO, System.currentTimeMillis());
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            resultVo = new ResultVo(e.getBalance(), System.currentTimeMillis());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            // do nothing, return null
            boolean isBullBullGame = ordersDto.getGameId().equals(GameCode.BULL_BULL);

            if (isBullBullGame) {
                this.prepareSettleCredit(ordersDto, paramsDto, gameSession, resultType);
            } else {
                this.prepareSettleBet(ordersDto, gameSession, resultType);
            }
            resultVo = new ResultVo(BigDecimal.ZERO, System.currentTimeMillis());
            httpService.logError(httpRequestLog, e);
        } finally {
            if (!isRequestExists) {
                // first request (not request exist) will delete log after process finish.
                requestIdempotentLogService.delete(ordersDto, paramsDto.getLoginId());
            }
            httpService.end(httpRequestLog, new CommonVo());
        }
        return resultVo;
    }


    @ExceptionHandler({InvalidRequestException.class, InvalidPlayerException.class,
            AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonVo commonVo, HttpRequestLog httpRequestLog) {


        if (e instanceof InvalidRequestException) {
            commonVo.setErrorResponse(httpRequestLog.getId(),
                    ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message);
        } else if (e instanceof InsufficientBalanceException) {
            commonVo.setErrorResponse(httpRequestLog.getId(),
                    ResponseCodes.INSUFFICIENT_BALANCE.code,
                    ResponseCodes.INSUFFICIENT_BALANCE.message);
            httpService.logError(httpRequestLog, e);
        } else if (e instanceof InvalidPlayerException) {
            commonVo.setErrorResponse(httpRequestLog.getId(),
                    ResponseCodes.PLAYER_INVALID.code,
                    ResponseCodes.PLAYER_INVALID.message);
        } else if (e instanceof AuthenticationException) {
            commonVo.setErrorResponse(httpRequestLog.getId(),
                    ResponseCodes.AUTH_INVALID.code,
                    ResponseCodes.AUTH_INVALID.message);
        } else {
            commonVo.setErrorResponse(httpRequestLog.getId(),
                    ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message);
        }
        httpService.logError(httpRequestLog, e);
    }

    //Process betResult retry settledto
    private void prepareSettleBet(OrdersDto ordersDto, GameSession gameSession, ResultType resultType) {
        ExecutorService executor = vendorService.createThreadPool(ThreadSize.SETTLE_THREAD_SIZE);
        executor.submit(() -> {
            GeneralSettleDto generalSettleDto = new ModelMapper().map(ordersDto, GeneralSettleDto.class);
            betActionLogService.create(new Gson().toJson(generalSettleDto), generalSettleDto.getRoundId(),
                    generalSettleDto.getVendorBetId(), generalSettleDto.getExternalTransactionId(), gameSession,
                    2, resultType);
        });
    }

    //prepare retry credit dto
    private void prepareSettleCredit(OrdersDto ordersDto, ParamsDto paramsDto, GameSession gameSession,
                                     ResultType resultType) {
        ExecutorService executor = vendorService.createThreadPool(ThreadSize.SETTLE_THREAD_SIZE);
        executor.submit(() -> {
            GeneralCreditDto generalCreditDto = new ModelMapper().map(ordersDto, GeneralCreditDto.class);
            generalCreditDto.setTakeAll(0);
            generalCreditDto.setTransferAmount(ordersDto.getAmount().abs());
            generalCreditDto.setVendorPlayerUsername(paramsDto.getLoginId());
            generalCreditDto.setTimestamp(System.currentTimeMillis());
            generalCreditDto.setToken(gameSession.getToken());
            generalCreditDto.setVendorGameCode(gameSession.getVendorGameCode());
            generalCreditDto.setVendorSettleTime(System.currentTimeMillis());
            betActionLogService.create(new Gson().toJson(generalCreditDto), generalCreditDto.getRoundId(),
                    generalCreditDto.getVendorBetId(), generalCreditDto.getExternalTransactionId(), gameSession,
                    3, resultType);
        });
    }

    private ResultVo createSuccessResultVo(GameSession gameSession, SettleDto settleDto, BigDecimal balance) {
        ResultVo resultVo = new ResultVo();
        resultVo.setUserId(gameSession.getVendorPlayerId());
        resultVo.setSn(settleDto.getParamsDto().getSn());
        resultVo.setAvailableAmount(balance);
        resultVo.setOrderResult("1");
        String tranId = settleDto.getParamsDto().getTranId();
        resultVo.setTranId(tranId != null && !tranId.trim().isEmpty() ? tranId : "null");
        return resultVo;
    }

    private GameSession getVendorPlayerId(UnsettledBet unsettledBet, WalletTransaction walletTransaction) throws
            InvalidPlayerException,
            AuthenticationException {

        GameSession gameSession = null;
        if (unsettledBet != null) {
            gameSession = gameSessionService.generateNewSessionTokenByVendorPlayerId(unsettledBet.getVendorPlayerId());
        }

        if (walletTransaction != null) {
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(walletTransaction.getVendorPlayerUsername());
        }

        return gameSession;
    }

}
