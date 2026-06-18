package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.result;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.data.kafka.betdetails.BetDetailEmitRequest;
import com.nextgen.gameaggregator.data.kafka.betdetails.EventKind;
import com.nextgen.gameaggregator.data.kafka.betdetails.RawBetDetailsProducer;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.service.PPPromoPayoutService;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Component
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
@RequiredArgsConstructor
public class ResultAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final PPPromoPayoutService promoPayoutService;
    private final RawBetDetailsProducer rawBetDetailsProducer;

    public ResponseVo resultRequest(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResultVo responseVo = new ResultVo();
        String traceId = httpRequestLog.getId();
        GameSession gameSession;
        ResultDto dto = new ResultDto();
        boolean isRequestExists = false;

        try {
            // Retrieve request body in original string format and convert into dto
            dto = HttpService.convertQueryStringToDto(httpRequestLog, ResultDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            //check for idempotent request
            if (requestIdempotentLogService.checkExists(dto, dto.getUserId()) == null) {
                requestIdempotentLogService.create(dto, dto.getUserId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 2. Verify session token
            try {
                gameSession = gameSessionService.verifyToken(dto.getToken());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGameId(), gameSession);
            } catch (AuthenticationException authenticationException) {
                gameSession = gameSessionService.generateNewSessionToken(dto.getUserId());
                gameSessionService.updateByVendorGameCode(gameSession, dto.getGameId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            if (promoPayoutService.isPromoTransaction(dto.getBonusCode())) {
                // TODO: need to add this to promo transaction history
                return promoPayoutService.getDefaultResponseForResult(traceId, gameSession.getVendorCurrencyCode());
            }

            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setBonus(BigDecimal.ZERO);

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Send win result to Operator
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, ResultType.BET_WIN, vendorService, httpRequestLog);

            this.emitRawBetDetail(gameSession, dto, httpRequestLog.getGaBetId(), httpRequestLog.getRequestBody());

            String transactionId = VendorService.getTransactionId(traceId);
            responseVo.setTransactionId(transactionId);
            responseVo.setCash(balance);

        } catch (InternalServerTimeoutRetryException internalServerTimeoutRetryException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, internalServerTimeoutRetryException);

        } catch (BetResultIdempotentViolationException idempotentViolationException) {
            // duplicate bet result received, do not process but return original transaction id back to vendor
            responseVo.setTransactionId(VendorService.getTransactionId(idempotentViolationException.getTransactionId()));
            responseVo.setCash(idempotentViolationException.getBalance());
            httpService.logError(httpRequestLog, idempotentViolationException);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, credentialNotFoundException);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);
            httpService.logError(httpRequestLog, invalidSignatureException);

        } catch (InvalidAgentApiCredentialException InvalidAgentApiCredentialException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
            httpService.logError(httpRequestLog, InvalidAgentApiCredentialException);

        } catch (BetNotFoundException betNotFoundException) {
            //update bet not found for result to retry due to vendor async send issue
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpRequestLog.setErrorMessage(betNotFoundException.getMessage());
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);
        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getUserId());
            }

            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void emitRawBetDetail(GameSession gameSession, ResultDto dto, String gaBetId, String body) {
        String vendorBetId = dto.getVendorBetId();
        String roundId = dto.getRoundId();
        if (gameSession == null) {
            log.warn("Skipping {} raw bet detail emit: gameSession is null vendorBetId={} roundId={}",
                    Endpoints.VENDOR, vendorBetId, roundId);
            return;
        }
        try {
            rawBetDetailsProducer.emit(BetDetailEmitRequest.builder()
                    .vendor(Endpoints.VENDOR)
                    .eventKind(EventKind.RESULT_UPDATE)
                    .vendorBetId(vendorBetId)
                    .gaBetId(gaBetId)
                    .roundId(roundId)
                    .vendorPlayerUsername(gameSession.getVendorPlayerUsername())
                    .agentId(gameSession.getAgentId())
                    .gameCategoryId(gameSession.getGameCategoryId())
                    .bodyFormat(Endpoints.BODY_FORMAT)
                    .requestBody(body)
                    .build());
        } catch (Exception e) {
            log.warn("{} raw bet detail emit failed vendorBetId={} roundId={}: {}",
                    Endpoints.VENDOR, vendorBetId, roundId, e.getMessage());
        }
    }

    private void doValidation(ResultDto dto) throws InvalidRequestException, InvalidPlayerException {

        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }

    private void doVerification(HttpRequestLog request, ResultDto dto, GameSession gameSession) throws
            InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException, AuthenticationException {

        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // 3. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 4. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);
    }
}
