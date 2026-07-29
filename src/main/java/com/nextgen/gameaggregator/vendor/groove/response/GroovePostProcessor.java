package com.nextgen.gameaggregator.vendor.groove.response;

import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.InvalidRequestContext;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.vendor.groove.api.authenticate.AuthenticateRequest;
import com.nextgen.gameaggregator.vendor.groove.api.balance.BalanceRequest;
import com.nextgen.gameaggregator.vendor.groove.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.groove.api.betandresult.BetAndResultRequest;
import com.nextgen.gameaggregator.vendor.groove.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.groove.api.rollback.RollbackRequest;
import com.nextgen.gameaggregator.vendor.groove.config.GrooveConfig;
import com.nextgen.gameaggregator.vendor.groove.constant.GameMode;
import com.nextgen.gameaggregator.vendor.groove.constant.OrderType;
import com.nextgen.gameaggregator.vendor.groove.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.groove.util.VendorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.nextgen.gameaggregator.vendor.groove.util.VendorUtil.formatBalance;

@Component
@Slf4j
public class GroovePostProcessor implements VendorResponsePostProcessor {

    // List of request classes this processor handles
    private static final List<Class<?>> REQUEST_CLASSES = List.of(
            AuthenticateRequest.class,
            BalanceRequest.class,
            BetRequest.class,
            BetResultRequest.class,
            BetAndResultRequest.class,
            RollbackRequest.class
    );

    private final WalletBalanceService walletBalanceService;
    private final GameTransactionService gameTransactionService;
    private static final String API_VERSION = "apiversion";

    public GroovePostProcessor(WalletBalanceService walletBalanceService,
                               GameTransactionService gameTransactionService) {
        this.walletBalanceService = walletBalanceService;
        this.gameTransactionService = gameTransactionService;
    }

    @Override
    public String getVendorClassName() {
        return GrooveConfig.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse postProcessInvalidRequest(InvalidRequestContext ctx) {
        Map<String, Object> responseBody = new HashMap<>(ctx.getResponseBody());
        String apiVersion = ctx.getQueryParam(API_VERSION).orElse("1.0");

        responseBody.put(API_VERSION, apiVersion);
        return new VendorErrorResponse(HttpStatus.OK, responseBody);
    }

    /**
     * Enrich the response based on the type of request
     */
    @Override
    public VendorErrorResponse postProcessErrorResponse(VendorErrorResponse errorResponse,
                                                        VendorExceptionContext errorContext) {
        log.debug("Post-processing error response for vendor: {}", getVendorClassName());

        if (!(errorResponse.getBody() instanceof ErrorResponse response)) {
            return errorResponse;
        }

        Optional<Object> requestOpt = errorContext.getAnyPresentClass(REQUEST_CLASSES);
        String apiVersion = null;

        if (requestOpt.isPresent()) {
            apiVersion = extractApiVersionViaReflection(requestOpt.get());
            response.setApiversion(apiVersion);
        }

        if (!ResponseCode.DUPLICATE_SUCCESS.message.equals(response.getStatus())) {
            return errorResponse;
        }

        if (requestOpt.isPresent()) {
            Object request = requestOpt.get();
            if (request instanceof BetRequest betReq) {
                String docId = GameTransaction.createDocId(getVendorClassName(), TxnType.BET, betReq.getTransactionid());
                Optional<GameTransaction> txnOpt = gameTransactionService.get(docId);

                if (txnOpt.isPresent()) {
                    GameTransaction txn = txnOpt.get();

                    boolean usernameMismatch = !txn.getUsername().equals(betReq.getAccountid());
                    boolean betAmountMismatch = betReq.getBetamount() != null && txn.getBetAmount().compareTo(betReq.getBetamount()) != 0;

                    if (usernameMismatch || betAmountMismatch) {
                        ErrorResponse bareResponse = getErrorResponse(ResponseCode.TXN_OPERATOR_MISMATCH, apiVersion);
                        return new VendorErrorResponse(HttpStatus.OK, bareResponse);
                    }
                }
            }else if (request instanceof BetResultRequest resultReq) {
                String docId = GameTransaction.createDocId(getVendorClassName(), TxnType.RESULT, resultReq.getTransactionid());
                Optional<GameTransaction> txnOpt = gameTransactionService.get(docId);

                if (txnOpt.isPresent()) {
                    GameTransaction txn = txnOpt.get();

                    boolean usernameMismatch = !txn.getUsername().equals(resultReq.getAccountid());
                    boolean winAmountMismatch = resultReq.getResult() != null && txn.getWinAmount().compareTo(resultReq.getResult()) != 0;

                    if (usernameMismatch || winAmountMismatch) {
                        ErrorResponse bareResponse = getErrorResponse(ResponseCode.TXN_OPERATOR_MISMATCH, apiVersion);
                        return new VendorErrorResponse(HttpStatus.OK, bareResponse);
                    }
                }
            }else if (request instanceof BetAndResultRequest betAndResultReq) {
                String docId = GameTransaction.createDocId(getVendorClassName(), TxnType.BET_N_RESULT, betAndResultReq.getTransactionid());
                Optional<GameTransaction> txnOpt = gameTransactionService.get(docId);

                if (txnOpt.isPresent()) {
                    GameTransaction txn = txnOpt.get();

                    boolean usernameMismatch = !txn.getUsername().equals(betAndResultReq.getAccountid());
                    boolean betAmountMismatch = betAndResultReq.getBetamount() != null && txn.getBetAmount().compareTo(betAndResultReq.getBetamount()) != 0;
                    boolean winAmountMismatch = betAndResultReq.getResult() != null && txn.getWinAmount().compareTo(betAndResultReq.getResult()) != 0;

                    if (usernameMismatch || betAmountMismatch || winAmountMismatch) {
                        ErrorResponse bareResponse = getErrorResponse(ResponseCode.TXN_OPERATOR_MISMATCH, apiVersion);
                        return new VendorErrorResponse(HttpStatus.OK, bareResponse);
                    }
                }
            }

            enrichResponse(response, request, errorContext);
        }
        return errorResponse;
    }

    private void enrichResponse(ErrorResponse response, Object request, VendorExceptionContext context) {
        String sessionId = null;
        String transactionId = null;

        if (request instanceof BetRequest req) {
            sessionId = req.getGamesessionid();
            transactionId = req.getTransactionid();
            response.setRealmoneybet(formatBalance(req.getBetamount()));
            response.setBonusmoneybet(formatBalance(BigDecimal.ZERO));

        } else if (request instanceof BetResultRequest req) {
            sessionId = req.getGamesessionid();
            transactionId = req.getTransactionid();
            response.setBonusWin(formatBalance(BigDecimal.ZERO));
            response.setRealMoneyWin(formatBalance(req.getResult()));
            response.setWalletTx(transactionId);

        } else if (request instanceof BetAndResultRequest req) {
            sessionId = req.getGamesessionid();
            transactionId = req.getTransactionid();
            response.setRealmoneybet(formatBalance(req.getBetamount()));
            response.setBonusmoneybet(formatBalance(BigDecimal.ZERO));
            response.setBonusWin(formatBalance(BigDecimal.ZERO));
            response.setRealMoneyWin(formatBalance(req.getResult()));
            response.setWalletTx(transactionId);

        } else if (request instanceof RollbackRequest req) {
            sessionId = req.getGamesessionid();
            transactionId = req.getTransactionid();
        }

        if (sessionId != null) {
            String token = VendorUtil.extractTokenFromSessionId(sessionId);
            PlayerBalanceData balanceData = getPlayerBalanceDataByToken(token);

            response.setBalance(formatBalance(balanceData.getBalance()));
            response.setReal_balance(formatBalance(balanceData.getBalance()));
        }

        response.setAccounttransactionid(transactionId);
        response.setBonus_balance(formatBalance(BigDecimal.ZERO));
        response.setGame_mode(GameMode.REAL_MONEY.value);
        response.setOrder(OrderType.CASH_MONEY.value);
    }

    private String extractApiVersionViaReflection(Object request) {
        try {
            Method getApiVersionMethod = request.getClass().getMethod("getApiversion");
            Object value = getApiVersionMethod.invoke(request);
            if (value instanceof String apiVersion) {
                return apiVersion;
            }
        } catch (Exception e) {
            log.warn("Could not extract apiversion via reflection from request type: {}", request.getClass().getName());
        }
        return null;
    }

    private PlayerBalanceData getPlayerBalanceDataByToken(String token) {
        BalanceContext balanceContext = null;

        try {
            if (token != null) {
                balanceContext = BalanceContext.builder()
                        .vendorSessionToken(token)
                        .build();

            }
        }
        catch (Exception e) {
            log.error(ResponseCode.NOT_LOGGED_ON.message);
        }

        return walletBalanceService.process(balanceContext);
    }

    private ErrorResponse getErrorResponse(ResponseCode responseCode, String apiVersion) {
        return ErrorResponse.builder()
                .code(responseCode.code)
                .status(responseCode.message)
                .message(responseCode.message)
                .apiversion(apiVersion)
                .build();
    }
}
