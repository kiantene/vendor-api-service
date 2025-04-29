package com.nextgen.gameaggregator.vendor.bglive.api.bet;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralCreditDto;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralRollbackDto;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.GameCode;
import com.nextgen.gameaggregator.vendor.bglive.constant.NiuBetMagnification;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

@Service
public class BetService {
    private static final ConcurrentLinkedQueue<String> errorOrderIds = new ConcurrentLinkedQueue<>();
    private final ModelMapper modelMapper = new ModelMapper();
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final BetActionLogService betActionLogService;
    private final OperatorWalletService operatorWalletService;
    private final WalletTransactionBetHistoryService walletTransactionBetHistoryService;
    private final WalletRequestService walletRequestService;

    public BetService(HttpService httpService,
                      WalletService walletService,
                      GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      AgentPlayerService agentPlayerService,
                      VendorService vendorService,
                      BetActionLogService betActionLogService,
                      OperatorWalletService operatorWalletService,
                      WalletTransactionBetHistoryService walletTransactionBetHistoryService,
                      WalletRequestService walletRequestService) {

        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
        this.betActionLogService = betActionLogService;
        this.operatorWalletService = operatorWalletService;
        this.walletTransactionBetHistoryService = walletTransactionBetHistoryService;
        this.walletRequestService = walletRequestService;
    }

    public CommonVo bet(HttpRequestLog httpRequestLog, HttpServletRequest httpServletRequest) {
        CommonVo commonVo = new CommonVo();
        String traceId = httpRequestLog.getId();
        boolean processFailed = false;
        BetDto betDto = null;
        ExecutorService executor = null;
        GameSession gameSession = null;
        BigDecimal balance;
        try {
            String body = httpRequestLog.getRequestBody();
            betDto = HttpService.convertJsonToDto(body, BetDto.class);
            int orderCount = betDto.getParamsDto().getOrders().size();
            executor = vendorService.createThreadPool(orderCount);
            // Handle the action and return the resulting value
            this.doValidation(betDto);
            gameSession = this.getGameSession(betDto.getParamsDto().getLoginId());
            this.doVerification(betDto, gameSession);
            //process bet
            List<CompletableFuture<ResultVo>> resultVoList = this.processAllOrders(betDto, httpServletRequest,
                    body, executor);

            //check all orders balance
            balance = vendorService.checkResponseAndReturnBalance(resultVoList, traceId, gameSession, httpRequestLog);
            if (balance == null) {
                processFailed = true;
                throw new BetFailedException("Have Transaction Failed");
            }

            ResultVo resultVo = this.createSuccessResultVo(gameSession, betDto, balance);
            commonVo.setSuccessResponse(betDto.getId(), resultVo);

        } catch (InsufficientBalanceException e) {
            processFailed = true;
            commonVo.setErrorResponse(httpRequestLog.getId(),
                    ResponseCodes.INSUFFICIENT_BALANCE.code,
                    ResponseCodes.INSUFFICIENT_BALANCE.message);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            this.handleException(e, commonVo, httpRequestLog);
        } finally {
            if (executor != null) {
                executor.shutdown();
            }

            // Rollback transactions if processing failed
            if (processFailed) {
                this.handleRollbackForFailedBets(betDto, gameSession);
                httpService.end(httpRequestLog, new CommonVo());
            }
        }
        return commonVo;
    }

    private void doValidation(BetDto betDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(betDto);

        ParamsDto paramsDto = betDto.getParamsDto();
        if (paramsDto != null) {
            ValidationUtils.validateRequest(paramsDto);

            List<OrdersDto> ordersList = paramsDto.getOrders();
            if (ordersList == null || ordersList.isEmpty()) {
                throw new InvalidRequestException("Bet request must contain at least one order.");
            }
            for (OrdersDto order : ordersList) {
                ValidationUtils.validateRequest(order);
            }
        }
    }

    private void doVerification(BetDto betDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            CredentialNotFoundException,
            InvalidFormatException,
            InvalidTokenException {

        String snCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SN_CODE);
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        // Verify received sn code is the same from game session
        ValidationUtils.isEquals(snCode, betDto.getParamsDto().getSn(), InvalidTokenException::new);

        String validateSign = VendorService.encryptBetMd5Key(betDto.getParamsDto().getRandom(), snCode,
                gameSession.getVendorPlayerUsername(), String.valueOf(betDto.getParamsDto().getAmount()), secretKey);
        ValidationUtils.isEquals(validateSign, betDto.getParamsDto().getSign(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }

    private GameSession getGameSession(String loginId) throws AuthenticationException {
        return gameSessionService.getGameSessionByVendorPlayerUsername(loginId);
    }

    //Concurrent process orders
    private ResultVo processData(OrdersDto ordersDto, HttpServletRequest httpServletRequest, String body,
                                 GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(httpServletRequest);
        httpRequestLog.setRequestBody(new Gson().toJson(ordersDto));
        String traceId = httpRequestLog.getId();
        ResultVo resultVo = null;
        WalletRequest walletRequest = null;
        try {
            this.doValidation(ordersDto);

            // Verify session token
            String gameCode = VendorService.getGameCode(ordersDto.getIssueId());
            if (!gameCode.equals(gameSession.getVendorGameCode())) {
                vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(gameCode, gameSession);
            }
            boolean isBullBullGame = ordersDto.getGameId().equals(GameCode.BULL_BULL);
            if (isBullBullGame) {
                walletRequest = WalletRequestService.init(httpRequestLog);
                boolean isDoublePlay = VendorService.isDoublePlay(Long.parseLong(ordersDto.getPlayId()));
                if (isDoublePlay) {
                    BigDecimal doublePlayAmount = ordersDto.getBetAmount().
                            multiply(BigDecimal.valueOf(NiuBetMagnification.NIU_BET_MAGNIFICATION));
                    ordersDto.setAmount(doublePlayAmount);
                }

                // add request idempotent check
                httpService.isDuplicateRequest(ordersDto);

                WalletRequest currentWalletRequest = new WalletRequest(walletRequest);
                vendorService.dataDebitMapper(currentWalletRequest, ordersDto, gameSession);
                walletRequest = operatorWalletService.betDebit(currentWalletRequest);
                //create wallet transaction bet history
                walletTransactionBetHistoryService.create(currentWalletRequest, gameSession);
                resultVo = new ResultVo(walletRequest.getBalanceAfter());
            } else {
                BetEvent betEvent = walletService.processBet(traceId, gameSession, ordersDto, body, httpRequestLog);
                resultVo = new ResultVo(betEvent.getLastBalance());
            }
        } catch (InsufficientBalanceException e) {
            resultVo = new ResultVo(BigDecimal.ONE.negate());
            errorOrderIds.add(ordersDto.getExternalTransactionId());

            httpService.logError(httpRequestLog, e);
        } catch (DuplicateRequestException | BetResultIdempotentViolationException e) {
            resultVo = new ResultVo(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            // do nothing, return null
            if (ordersDto.getGameId().equals(GameCode.BULL_BULL)) {
                errorOrderIds.add(ordersDto.getExternalTransactionId());
            }
            httpService.logError(httpRequestLog, e);
        } finally {
            if (ordersDto.getGameId().equals(GameCode.BULL_BULL)) {
                walletRequestService.end(walletRequest, httpRequestLog, new CommonVo());
            }
            httpService.end(httpRequestLog, new CommonVo());
        }
        return resultVo;
    }

    //process all orders
    private List<CompletableFuture<ResultVo>> processAllOrders(BetDto betDto, HttpServletRequest httpServletRequest,
                                                               String body, ExecutorService executor) throws
            AuthenticationException {
        List<CompletableFuture<ResultVo>> resultVoList = new LinkedList<>();

        for (OrdersDto order : betDto.getParamsDto().getOrders()) {
            GameSession orderGameSession = this.getGameSession(betDto.getParamsDto().getLoginId());
            CompletableFuture<ResultVo> resultVo = CompletableFuture.supplyAsync(
                    () -> this.processData(order, httpServletRequest, body, orderGameSession),
                    executor);
            resultVoList.add(resultVo);
        }

        return resultVoList;
    }

    private ResultVo createSuccessResultVo(GameSession gameSession, BetDto betDto, BigDecimal balance) {
        ResultVo resultVo = new ResultVo();
        resultVo.setUserId(gameSession.getVendorPlayerId());
        resultVo.setSn(betDto.getParamsDto().getSn());
        resultVo.setAvailableAmount(balance);
        resultVo.setOrderResult("1");
        String tranId = betDto.getParamsDto().getTranId();
        resultVo.setTranId(tranId != null && !tranId.trim().isEmpty() ? tranId : "null");
        return resultVo;
    }

    @ExceptionHandler({InvalidRequestException.class, InvalidPlayerException.class,
            AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonVo commonVo, HttpRequestLog httpRequestLog) {

        if (e instanceof InvalidRequestException) {
            commonVo.setErrorResponse(httpRequestLog.getId(),
                    ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message);
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

    //Exception figure which is bull game
    private void handleRollbackForFailedBets(BetDto betDto, GameSession gameSession) {
        boolean isBullBullGame = betDto.getParamsDto().getOrders().stream()
                .anyMatch(o -> GameCode.BULL_BULL.equals(o.getGameId()));

        if (isBullBullGame) {
            this.prepareDebitRollback(betDto.getParamsDto().getOrders(), betDto, gameSession);
        } else {
            this.prepareRollback(betDto.getParamsDto().getOrders(), gameSession);
        }
    }

    private void doValidation(OrdersDto ordersDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(ordersDto);
    }

    //process bet win lose
    private void prepareRollback(List<OrdersDto> ordersDtoList, GameSession gameSession) {
        int orderCount = ordersDtoList.size();
        ExecutorService executor = vendorService.createThreadPool(orderCount);
        for (OrdersDto ordersDto : ordersDtoList) {
            if (shouldSkipOrder(ordersDto)) {
                continue;
            }
            executor.submit(() -> {
                        GeneralRollbackDto generalRollbackDto = new GeneralRollbackDto();
                        generalRollbackDto.setRollbackId(ordersDto.getExternalTransactionId());
                        generalRollbackDto.setVendorSettledTime(null);
                        generalRollbackDto.setRoundId(ordersDto.getRoundId());
                        betActionLogService.create(new Gson().toJson(generalRollbackDto), ordersDto.getRoundId(),
                                ordersDto.getVendorBetId(), ordersDto.getExternalTransactionId(), gameSession, 1,
                                null);
                    }
            );
        }
    }

    // process credit
    private void prepareDebitRollback(List<OrdersDto> ordersDtoList, BetDto betDto, GameSession gameSession) {
        int orderCount = ordersDtoList.size();
        ExecutorService executor = vendorService.createThreadPool(orderCount);
        for (OrdersDto ordersDto : ordersDtoList) {
            if (shouldSkipOrder(ordersDto)) {
                continue;
            }
            executor.submit(() -> {
                GeneralCreditDto generalCreditDto = this.mapToGeneralCreditDto(ordersDto, betDto, gameSession);
                betActionLogService.create(new Gson().toJson(generalCreditDto), generalCreditDto.getRoundId(),
                        generalCreditDto.getVendorBetId(), generalCreditDto.getExternalTransactionId(), gameSession, 3,
                        null);
            });
        }
    }

    //prepare dto to rollback
    private GeneralCreditDto mapToGeneralCreditDto(OrdersDto ordersDto, BetDto betDto, GameSession gameSession) {
        GeneralCreditDto generalCreditDto = modelMapper.map(ordersDto, GeneralCreditDto.class);
        generalCreditDto.setTakeAll(0);
        generalCreditDto.setTransferAmount(ordersDto.getAmount().abs());
        generalCreditDto.setVendorPlayerUsername(betDto.getParamsDto().getLoginId());
        generalCreditDto.setTimestamp(System.currentTimeMillis());
        generalCreditDto.setToken(gameSession.getToken());
        generalCreditDto.setVendorGameCode(gameSession.getVendorGameCode());
        generalCreditDto.setVendorSettleTime(System.currentTimeMillis());
        generalCreditDto.setIsRollBack(1);
        return generalCreditDto;
    }

    private boolean shouldSkipOrder(OrdersDto ordersDto) {
        String transactionId = ordersDto.getExternalTransactionId();
        if (errorOrderIds.contains(transactionId)) {
            errorOrderIds.remove(transactionId);
            return true;
        }
        return false;
    }
}
