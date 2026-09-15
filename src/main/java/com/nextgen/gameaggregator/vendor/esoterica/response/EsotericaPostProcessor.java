package com.nextgen.gameaggregator.vendor.esoterica.response;

import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.InvalidRequestContext;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.vendor.esoterica.api.authenticate.AuthenticateRequest;
import com.nextgen.gameaggregator.vendor.esoterica.api.balance.BalanceRequest;
import com.nextgen.gameaggregator.vendor.esoterica.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.esoterica.api.betandresult.BetAndResultRequest;
import com.nextgen.gameaggregator.vendor.esoterica.api.endround.EndRoundRequest;
import com.nextgen.gameaggregator.vendor.esoterica.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.esoterica.api.rollback.RollbackRequest;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.esoterica.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.esoterica.util.VendorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class EsotericaPostProcessor implements VendorResponsePostProcessor {

    // List of request classes this processor handles
    private static final List<Class<?>> REQUEST_CLASSES = List.of(
            AuthenticateRequest.class,
            BalanceRequest.class,
            BetRequest.class,
            BetResultRequest.class,
            RollbackRequest.class,
            BetAndResultRequest.class,
            EndRoundRequest.class,
            Map.class
    );

    private final WalletBalanceService walletBalanceService;
    private final GameSessionService gameSessionService;
    private final GameTransactionService gameTransactionService;

    public EsotericaPostProcessor(WalletBalanceService walletBalanceService,
                                  GameSessionService gameSessionService,
                                  GameTransactionService gameTransactionService) {
        this.walletBalanceService = walletBalanceService;
        this.gameSessionService = gameSessionService;
        this.gameTransactionService = gameTransactionService;
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse postProcessErrorResponse(VendorErrorResponse errorResponse, VendorExceptionContext errorContext) {

        Optional<Object> requestOptional = errorContext.getAnyPresentClass(REQUEST_CLASSES);

        if (requestOptional.isEmpty()) {
            return errorResponse;
        }

        Object request = requestOptional.get();

        if (!(errorResponse.getBody() instanceof ErrorResponse responseVo)) {
            return errorResponse;
        }

        if (request instanceof AuthenticateRequest authenticateRequest) {
            enrichErrorResponse(responseVo, authenticateRequest);
        }
        else if (request instanceof BalanceRequest balanceRequest) {
            enrichErrorResponse(responseVo, balanceRequest);
        }
        else if (request instanceof BetRequest betRequest) {
            enrichErrorResponse(responseVo, betRequest);
            validateTransaction(responseVo, betRequest);
        }
        else if (request instanceof BetResultRequest betResultRequest) {
            enrichErrorResponse(responseVo, betResultRequest);
            validateTransaction(responseVo, betResultRequest);
        }
        else if (request instanceof RollbackRequest rollbackRequest) {
            enrichErrorResponse(responseVo, rollbackRequest);
        }
        else if (request instanceof BetAndResultRequest betAndResultRequest) {
            BetAndResultErrorResponse betAndResultErrorResponse = enrichBetAndResultErrorResponse(responseVo, betAndResultRequest);
            return new VendorErrorResponse(betAndResultErrorResponse);
        }
        else if (request instanceof EndRoundRequest endRoundRequest) {
            enrichErrorResponse(responseVo, endRoundRequest);
        }

        return errorResponse;
    }

    @Override
    public VendorErrorResponse postProcessInvalidRequest(InvalidRequestContext ctx) {

        CommonResponse commonResponse = new CommonResponse();
        Map<String, String> parsedFields = ctx.getParsedFields();
        String userId;

        if  (parsedFields.containsKey(StringConstants.BET_USER_ID)) {
            userId = parsedFields.get(StringConstants.BET_USER_ID);
        } else if (parsedFields.containsKey(StringConstants.WIN_USER_ID)) {
            userId = parsedFields.get(StringConstants.WIN_USER_ID);
        } else {
            userId = parsedFields.get(StringConstants.USER_ID);
        }

        PlayerBalanceData balanceData = getPlayerBalanceDataByUserId(userId);

        switch (ctx.getRequestUri().orElse("")) {
            case EndPoints.PATH + EndPoints.AUTHENTICATE:
                balanceData = getPlayerBalanceDataByToken(parsedFields.get(StringConstants.TOKEN));

                commonResponse = VendorUtil.enrichPostProcessInvalidRequest(
                        null,
                        balanceData,
                        ctx.getResponseBody(),
                        true,
                        false,
                        EndPoints.AUTHENTICATE);
                break;


            case EndPoints.PATH + EndPoints.BALANCE:

                commonResponse = VendorUtil.enrichPostProcessInvalidRequest(
                        null,
                        balanceData,
                        ctx.getResponseBody(),
                        true,
                        false,
                        EndPoints.BALANCE);
                break;

            case EndPoints.PATH + EndPoints.BET:

                commonResponse = VendorUtil.enrichPostProcessInvalidRequest(
                        parsedFields.get(StringConstants.REFERENCE),
                        balanceData,
                        ctx.getResponseBody(),
                        true,
                        true,
                        EndPoints.BET);
                break;

            case EndPoints.PATH + EndPoints.RESULT:

                commonResponse = VendorUtil.enrichPostProcessInvalidRequest(
                        parsedFields.get(StringConstants.REFERENCE),
                        balanceData,
                        ctx.getResponseBody(),
                        true,
                        false,
                        EndPoints.RESULT);
                break;


            case EndPoints.PATH + EndPoints.REFUND:

                commonResponse = VendorUtil.enrichPostProcessInvalidRequest(
                        parsedFields.get(StringConstants.REFERENCE),
                        balanceData,
                        ctx.getResponseBody(),
                        false,
                        false,
                        EndPoints.REFUND);
                break;

            case EndPoints.PATH + EndPoints.BETANDRESULT:

                CommonResponse bet = VendorUtil.enrichPostProcessInvalidRequest(
                        parsedFields.get(StringConstants.BET_REFERENCE),
                        balanceData,
                        ctx.getResponseBody(),
                        true,
                        true,
                        EndPoints.BETANDRESULT);

                CommonResponse win = VendorUtil.enrichPostProcessInvalidRequest(
                        parsedFields.get(StringConstants.WIN_REFERENCE),
                        balanceData,
                        ctx.getResponseBody(),
                        true,
                        false,
                        EndPoints.BETANDRESULT);

                Object descriptionObj = ctx.getResponseBody().get(StringConstants.DESCRIPTION);

                BetAndResultErrorResponse betAndResult = BetAndResultErrorResponse.builder()
                        .bet(bet)
                        .win(win)
                        .error((Integer) ctx.getResponseBody().get(StringConstants.ERROR))
                        .description(descriptionObj != null ? descriptionObj.toString() : null)
                        .build();

                return new VendorErrorResponse(betAndResult);

            case EndPoints.PATH + EndPoints.ENDROUND:

                commonResponse = VendorUtil.enrichPostProcessInvalidRequest(
                        null,
                        balanceData,
                        ctx.getResponseBody(),
                        false,
                        false,
                        EndPoints.ENDROUND);
                break;

            default:
                break;
        }

        return new VendorErrorResponse(commonResponse);
    }

    private void enrichErrorResponse(ErrorResponse response, AuthenticateRequest authenticateRequest) {

        PlayerBalanceData balanceData = getPlayerBalanceDataByToken(authenticateRequest.getToken());
        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;

        response.setCash(balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO);
        response.setBonus(BigDecimal.ZERO);
        response.setCurrency(balanceData != null ? balanceData.getCurrency() : null);
    }

    private void enrichErrorResponse(ErrorResponse response, BalanceRequest balanceRequest) {

        PlayerBalanceData balanceData = getPlayerBalanceDataByUserId(balanceRequest.getUserId());
        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;

        response.setCash(balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO);
        response.setBonus(BigDecimal.ZERO);
        response.setCurrency(balanceData != null ? balanceData.getCurrency() : null);
    }

    private void enrichErrorResponse(ErrorResponse response, BetRequest betRequest) {

        PlayerBalanceData balanceData = getPlayerBalanceDataByUserId(betRequest.getUserId());
        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;

        response.setTransactionId(betRequest.getReference());
        response.setCash(balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO);
        response.setBonus(BigDecimal.ZERO);
        response.setCurrency(balanceData != null ? balanceData.getCurrency() : null);
        response.setUsedPromo(BigDecimal.ZERO);
    }

    private void enrichErrorResponse(ErrorResponse response, BetResultRequest betResultRequest) {

        PlayerBalanceData balanceData = getPlayerBalanceDataByUserId(betResultRequest.getUserId());
        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;

        response.setTransactionId(betResultRequest.getReference());
        response.setCash(balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO);
        response.setBonus(BigDecimal.ZERO);
        response.setCurrency(balanceData != null ? balanceData.getCurrency() : null);
    }

    private void enrichErrorResponse(ErrorResponse response, RollbackRequest rollbackRequest) {
        response.setTransactionId(rollbackRequest.getReference());
    }

    private void enrichErrorResponse(ErrorResponse response, EndRoundRequest endRoundRequest) {

        PlayerBalanceData balanceData = getPlayerBalanceDataByUserId(endRoundRequest.getUserId());
        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;

        response.setCash(balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO);
        response.setBonus(BigDecimal.ZERO);
    }

    private BetAndResultErrorResponse enrichBetAndResultErrorResponse(ErrorResponse response, BetAndResultRequest request) {

        PlayerBalanceData balanceData = getPlayerBalanceDataByUserId(request.getBet().getUserId());
        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;
        BigDecimal cash = balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO;

        CommonResponse betResponse = CommonResponse.builder()
                .transactionId(request.getBet() != null ? request.getBet().getReference() : "")
                .cash(cash)
                .bonus(BigDecimal.ZERO)
                .usedPromo(BigDecimal.ZERO)
                .currency(balanceData != null ? balanceData.getCurrency() : null)
                .error(response.getError())
                .description(response.getDescription())
                .build();

        CommonResponse resultResponse = CommonResponse.builder()
                .transactionId(request.getWin() != null ? request.getWin().getReference() : "")
                .cash(cash)
                .bonus(BigDecimal.ZERO)
                .currency(balanceData != null ? balanceData.getCurrency() : null)
                .error(response.getError())
                .description(response.getDescription())
                .build();

        return BetAndResultErrorResponse.builder()
                .bet(betResponse)
                .win(resultResponse)
                .error(response.getError())
                .description(response.getDescription())
                .build();
    }

    private PlayerBalanceData getPlayerBalanceDataByToken(String token) {
        PlayerBalanceData balanceData = null;

        try {
            if (token != null) {
                BalanceContext balanceContext = BalanceContext.builder()
                        .vendorSessionToken(token)
                        .build();

                GameSession gameSession = gameSessionService.verifyToken(token);
                balanceData = gameSession == null || gameSession.getStatus() == 0 ? null : walletBalanceService.process(balanceContext);
            }
        }
        catch (Exception e) {
            log.error(ResponseCodes.PLAYER_NOT_FOUND.getMessage(), e);
        }

        return balanceData;
    }

    private PlayerBalanceData getPlayerBalanceDataByUserId(String userId) {
        PlayerBalanceData balanceData = null;

        try {
            if (userId != null) {
                BalanceContext balanceContext = BalanceContext.builder()
                        .vendorPlayerUsername(userId)
                        .build();

                GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(userId);
                balanceData = gameSession == null || gameSession.getStatus() == 0 ? null : walletBalanceService.process(balanceContext);
            }
        }
        catch (Exception e) {
            log.error(ResponseCodes.PLAYER_NOT_FOUND.getMessage(), e);
        }

        return balanceData;
    }

    private void validateTransaction(ErrorResponse response, CommonRequest request) {
        String docId;
        boolean gameTerminated = false;
        GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(request.getUserId());

        if (request instanceof BetRequest) {
            docId = GameTransaction.createDocId(getVendorClassName(), TxnType.BET, request.getReference());

            if (gameSession == null || gameSession.getStatus() == 0) {
                gameTerminated = true;
            }
        }
        else {
            docId = GameTransaction.createDocId(getVendorClassName(), TxnType.RESULT, request.getReference());
        }

        Optional<GameTransaction> txnOpt = gameTransactionService.get(docId);
        String uniqueId = request.getUserId() + request.getGameName() + request.getRoundId();

        if (txnOpt.filter(txn -> !txn.isError()).isPresent()) {
            GameTransaction txn = txnOpt.get();

            if (!gameTerminated) {
                if (!uniqueId.equals(txn.getRoundId())) {
                    response.setError(ResponseCodes.INTERNAL_ERROR.getCode());
                    response.setDescription(ResponseCodes.INTERNAL_ERROR.getMessage());
                }
                else {
                    response.setError(ResponseCodes.DUPLICATE_REQUEST.getCode());
                    response.setDescription(ResponseCodes.DUPLICATE_REQUEST.getMessage());
                }
            }
            else {
                response.setError(ResponseCodes.AUTHENTICATION_FAILED.getCode());
                response.setDescription(ResponseCodes.AUTHENTICATION_FAILED.getMessage());
            }
        }
    }
}
