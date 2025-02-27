package com.nextgen.gameaggregator.vendor.bglive.api.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.GameCode;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bglive.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BetService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;

    @Autowired
    public BetService(HttpService httpService,
                      WalletService walletService,
                      GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      AgentPlayerService agentPlayerService,
                      VendorService vendorService, WalletRequestService walletRequestService, OperatorWalletService operatorWalletService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
    }

    public CommonVo bet(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
        try {
            WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
            String body = httpRequestLog.getRequestBody();
            BetDto betDto = HttpService.convertJsonToDto(body, BetDto.class);
            // Handle the action and return the resulting value
            this.doValidation(betDto);

            GameSession gameSession = getGameSession(betDto);

            this.doVerification(betDto, gameSession);
            //process bet
            processOrders(walletRequest, betDto, gameSession, traceId, httpRequestLog);

            ResultVo resultVo = new ResultVo();
            resultVo.setUserId(gameSession.getVendorPlayerId());
            resultVo.setSn(betDto.getParamsDto().getSn());
            resultVo.setAvailableAmount(walletService.getBalance(traceId, gameSession, httpRequestLog));
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

    private GameSession getGameSession(BetDto betDto) throws AuthenticationException {
        return gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getParamsDto().getLoginId());
    }

    //loop betdto's order
    private void processOrders(WalletRequest walletRequest, BetDto betDto, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog)
            throws InvalidFormatException,
            GameNotSupportedException,
            InvalidAgentApiCredentialException,
            VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException,
            InsufficientBalanceException,
            TransactionStillProcessingException,
            InvalidOperatorResponseException,
            CouchbaseDataIntegrityException,
            InternalServerException,
            InvalidRequestException,
            BetNotAllowedException {

        for (OrdersDto order : betDto.getParamsDto().getOrders()) {
            betDto.setCurrentOrder(order);
            boolean isBullBullGame = order.getGameId().equals(GameCode.BULL_BULL);
            String gameCode = VendorService.getGameCode(order.getIssueId());
            if (!gameCode.equals(gameSession.getVendorGameCode())) {
                vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(gameCode, gameSession);
            }
            if (isBullBullGame) {
                WalletRequest currentWalletRequest = new WalletRequest(walletRequest);
                dataDebitMapper(currentWalletRequest, betDto, gameSession);
                operatorWalletService.betDebit(currentWalletRequest);
                break;
            } else {
                walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
            }
        }
    }

    private void dataDebitMapper(WalletRequest walletRequest, BetDto betDto, GameSession gameSession) {

        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setExternalTransactionId(betDto.getRoundId());
        walletRequest.setRoundId(betDto.getRoundId());
        walletRequest.setVendorGameCode(betDto.getGameId());
        walletRequest.setTimestamp(System.currentTimeMillis());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(betDto.getVendorBetId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        BigDecimal amount = betDto.getParamsDto().getAmount().abs();
        walletRequest.setTransferAmount(amount);
        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
    }
}
