package com.nextgen.gameaggregator.vendor.spribe.api.rollback;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.spribe.service.VendorService;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import com.nextgen.gameaggregator.vendor.spribe.vo.DataVo;
import com.nextgen.gameaggregator.vendor.spribe.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = Endpoints.PATH)
public class RollbackAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final WalletRollbackServiceWrapper walletRollbackServiceWrapper;
    private final RollbackContextMapper rollbackContextMapper;

    @PostMapping(path = Endpoints.ROLLBACK)
    public ResponseVo rollback(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo vo = new ResponseVo();
        DataVo data = new DataVo();
        String userId = null;
        String currency = null;
        String provider = null;
        String providerTxId = null;
        boolean isRequestExists = false;
        RollbackDto dto = new RollbackDto();
        GameSession gameSession = null;

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            dto = HttpService.convertJsonToDto(body, RollbackDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Request idempotent checking.
            if (requestIdempotentLogService.checkExists(dto, dto.getUser_id()) == null) {
                requestIdempotentLogService.create(dto, dto.getUser_id());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            try {
                gameSession = gameSessionService.verifyVendorToken(dto.getSession_token());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGame(), gameSession);
            } catch (AuthenticationException authenticationException) {
                gameSession = gameSessionService.generateNewSessionToken(dto.getUser_id());
                gameSessionService.updateByVendorGameCode(gameSession, dto.getGame());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setVendorToken(dto.getSession_token());
                gameSession.setToken(dto.getSession_token());
            }

            // 5. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            userId = gameSession.getVendorPlayerUsername();
            currency = gameSession.getVendorCurrencyCode();
            provider = dto.getProvider();
            providerTxId = dto.getProvider_tx_id();

            // 8. Send rollback request to Operator
            WalletRequest walletRequest = walletService.processRollback(dto, gameSession, vendorService, httpRequestLog);

            // 9. Set response data
            data.setOperator_tx_id(traceId);
            data.setNew_balance(AmountConverter.convertBalanceToUnit(walletRequest.getBalanceAfter()));
            data.setOld_balance(AmountConverter.convertBalanceToUnit(walletRequest.getBalanceBefore()));
            data.setUser_id(userId);
            data.setCurrency(currency);
            data.setProvider(provider);
            data.setProvider_tx_id(providerTxId);
            vo.setErrorCode(ErrorCodes.SUCCESS);
            vo.setData(data);

        } catch (BetNotFoundException betNotFoundException) {
            vo.setErrorCode(ErrorCodes.TRANSACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, betNotFoundException);

            // find and insert settled bet to process rollback
            BetRollbackContext rollbackContext = rollbackContextMapper.toBetRollbackContext(dto);
            rollbackContext.setTraceId(traceId);
            rollbackContext.setGameSession(gameSession);
            rollbackContext.setVendorService(vendorService);
            rollbackContext.setHttpRequestLog(httpRequestLog);
            walletRollbackServiceWrapper
                    .initialise(rollbackContext)
                    .processAsync(rollbackContext);

        } catch (RecordNotFoundException recordNotFoundException) {
            vo.setErrorCode(ErrorCodes.TRANSACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, recordNotFoundException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_DUPLICATE_REQUEST.code) ||
                    invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_TRANSACTION_DUPLICATED.code)) {
                vo.setErrorCode(ErrorCodes.DUPLICATE_TRANSACTION);

            } else if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                vo.setErrorCode(ErrorCodes.INSUFFICIENT_FUND);

            } else {
                vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);

            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (AuthenticationException authenticationException) {
            vo.setErrorCode(ErrorCodes.INVALID_TOKEN);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InvalidRequestException |
                 InvalidAgentApiCredentialException |
                 TransactionStillProcessingException | VendorCurrencyNotSupportException |
                 GameNotSupportedException internalErrorException) {
            vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, internalErrorException);

        } catch (BetResultIdempotentViolationException |
                 BetRefundIdempotentViolationException idempotentViolationException) {
            data.setOperator_tx_id(traceId);
            data.setNew_balance(AmountConverter.convertBalanceToUnit(BigDecimal.ZERO));
            data.setOld_balance(AmountConverter.convertBalanceToUnit(BigDecimal.ZERO));
            data.setUser_id(userId);
            data.setCurrency(currency);
            data.setProvider(provider);
            data.setProvider_tx_id(providerTxId);
            vo.setErrorCode(ErrorCodes.DUPLICATE_TRANSACTION);
            vo.setData(data);
            httpService.logError(httpRequestLog, idempotentViolationException);

        } catch (Exception exception) {
            vo.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getUser_id());
            }
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollbackDto dto, GameSession gameSession) throws AuthenticationException, GameNotSupportedException {

        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUser_id(), AuthenticationException::new);

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGame()), GameNotSupportedException::new);
    }
}
