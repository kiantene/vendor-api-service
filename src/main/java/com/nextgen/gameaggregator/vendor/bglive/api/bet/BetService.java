package com.nextgen.gameaggregator.vendor.bglive.api.bet;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralRollbackDto;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bglive.vo.ResultVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class BetService {
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;
    private final BetActionLogService betActionLogService;

    public BetService(HttpService httpService,
                      WalletService walletService,
                      GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      AgentPlayerService agentPlayerService,
                      VendorService vendorService, WalletRequestService walletRequestService, OperatorWalletService operatorWalletService, BetActionLogService betActionLogService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
        this.betActionLogService = betActionLogService;
    }

    public CommonVo bet(HttpRequestLog httpRequestLog, HttpServletRequest httpServletRequest) {
        CommonVo commonVo = new CommonVo();
        boolean processFailed = false;
        BetDto betDto = null;
        GameSession gameSession = null;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_SIZE);
        try {
            WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
            String body = httpRequestLog.getRequestBody();
            betDto = HttpService.convertJsonToDto(body, BetDto.class);
            // Handle the action and return the resulting value
            this.doValidation(betDto);
            gameSession = getGameSession(betDto.getParamsDto().getLoginId());

            this.doVerification(betDto, gameSession);
            //process bet
            List<CompletableFuture<ResultVo>> resultVoList = new LinkedList<>();
            for (OrdersDto order : betDto.getParamsDto().getOrders()) {
                GameSession orderGameSession = getGameSession(betDto.getParamsDto().getLoginId());
                CompletableFuture<ResultVo> resultVo = CompletableFuture.supplyAsync(() -> processData(order, httpServletRequest, body, orderGameSession), executor);
                resultVoList.add(resultVo);
            }

            // Get latest balance, if have fail response then return error
            BigDecimal balance = vendorService.checkResponseAndReturnBalance(resultVoList);
            if (balance == null) {
                processFailed = true;
                throw new BetFailedException("Have Transaction Failed");
            }

            ResultVo resultVo = new ResultVo();
            resultVo.setUserId(gameSession.getVendorPlayerId());
            resultVo.setSn(betDto.getParamsDto().getSn());
            resultVo.setAvailableAmount(balance);
            resultVo.setOrderResult("1");
            String tranId = betDto.getParamsDto().getTranId();
            resultVo.setTranId(tranId != null && !tranId.trim().isEmpty() ? tranId : "null");
            commonVo.setSuccessResponse(betDto.getId(), resultVo);

        } catch (InsufficientBalanceException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.INSUFFICIENT_BALANCE.code,
                    ResponseCodes.INSUFFICIENT_BALANCE.message, ResponseCodes.INSUFFICIENT_BALANCE.message);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidPlayerException e) {

            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.PLAYER_INVALID.code,
                    ResponseCodes.PLAYER_INVALID.message, ResponseCodes.PLAYER_INVALID.message);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException e) {

            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.AUTH_INVALID.code,
                    ResponseCodes.AUTH_INVALID.message, ResponseCodes.AUTH_INVALID.message);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);

        } finally {
            // close executor
            executor.shutdown();
            // Failed then rollback all transaction
            if (processFailed) {
                //insert to collection for rollback all bet
                this.prepareRollback(betDto.getParamsDto().getOrders(), gameSession);
            }
            httpService.end(httpRequestLog, new CommonVo());
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
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidFormatException {

        String snCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SN_CODE);
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(snCode, betDto.getParamsDto().getSn(), InvalidPlayerException::new);

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

    private ResultVo processData(OrdersDto ordersDto, HttpServletRequest httpServletRequest, String body, GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(httpServletRequest);
        String traceId = httpRequestLog.getId();
        ResultVo resultVo = null;
        try {
            this.doValidation(ordersDto);

            // Verify session token
            String gameCode = VendorService.getGameCode(ordersDto.getIssueId());
            if (!gameCode.equals(gameSession.getVendorGameCode())) {
                vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(gameCode, gameSession);
            }
            // Process Result
            BetEvent betEvent = walletService.processBet(traceId, gameSession, ordersDto, body, httpRequestLog);
            resultVo = new ResultVo(betEvent.getLastBalance(), httpRequestLog.getOperatorTimestamp());

        } catch (BetResultIdempotentViolationException e) {
            resultVo = new ResultVo(e.getBalance(), httpRequestLog.getOperatorTimestamp());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            // do nothing, return null
            httpService.logError(httpRequestLog, e);
        }
        return resultVo;
    }


    private void doValidation(OrdersDto ordersDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(ordersDto);
    }

    private void prepareRollback(List<OrdersDto> ordersDtoList, GameSession gameSession) {
        THREAD_POOL.submit(() -> {
            for (OrdersDto ordersDto : ordersDtoList) {
                GeneralRollbackDto generalRollbackDto = new GeneralRollbackDto();
                generalRollbackDto.setRollbackId(ordersDto.getExternalTransactionId());
                generalRollbackDto.setVendorSettledTime(null);
                generalRollbackDto.setRoundId(ordersDto.getRoundId());
                betActionLogService.create(new Gson().toJson(generalRollbackDto), ordersDto.getRoundId(), ordersDto.getVendorBetId(), ordersDto.getExternalTransactionId(), gameSession, 1, null);
            }
        });
    }

//    private void dataDebitMapper(WalletRequest walletRequest, BetDto betDto, GameSession gameSession) {
//
//        walletRequestService.updateByGameSession(walletRequest, gameSession);
//        walletRequest.setExternalTransactionId(betDto.getRoundId());
//        walletRequest.setRoundId(betDto.getRoundId());
//        walletRequest.setVendorGameCode(betDto.getGameId());
//        walletRequest.setTimestamp(System.currentTimeMillis());
//        walletRequest.setToken(gameSession.getToken());
//        walletRequest.setVendorBetId(betDto.getVendorBetId());
//        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
//        BigDecimal amount = betDto.getParamsDto().getAmount().abs();
//        walletRequest.setTransferAmount(amount);
//        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
//    }
}
