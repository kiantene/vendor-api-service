package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.bet;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.data.kafka.betdetails.BetDetailEmitRequest;
import com.nextgen.gameaggregator.data.kafka.betdetails.EventKind;
import com.nextgen.gameaggregator.data.kafka.betdetails.RawBetDetailsProducer;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
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
@RequiredArgsConstructor
@Slf4j
public class BetAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final PPPromoPayoutService promoPayoutService;
    private final RawBetDetailsProducer rawBetDetailsProducer;

    public ResponseVo betRequest(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        BetVo responseVo = new BetVo();
        String traceId = httpRequestLog.getId();
        String vendorCurrencyCode = "";
        GameSession gameSession;
        boolean isRequestExists = false;
        BetDto dto = new BetDto();

        try {
            // Retrieve request body in original string format and convert into dto
            dto = HttpService.convertQueryStringToDto(httpRequestLog, BetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            //check for idempotent request
            if (requestIdempotentLogService.checkExists(dto, dto.getUserId()) == null) {
                requestIdempotentLogService.create(dto, dto.getUserId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 2. Retrieve and verify session token
            gameSession = gameSessionService.verifyToken(dto.getToken());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGameId(), gameSession);
            vendorCurrencyCode = gameSession.getVendorCurrencyCode();

            if (promoPayoutService.isPromoTransaction(dto.getBonusCode())) {
                // TODO: need to add this to promo transaction history
                return promoPayoutService.getDefaultResponseForBet(traceId, vendorCurrencyCode);
            }

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Process unsettled bet process
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, ResultType.BET_LOSE, vendorService, httpRequestLog);

            this.emitRawBetDetail(gameSession, dto, httpRequestLog.getGaBetId(), httpRequestLog.getRequestBody());

            String transactionId = VendorService.getTransactionId(traceId);
            responseVo.setTransactionId(transactionId);
            responseVo.setCurrency(vendorCurrencyCode);
            responseVo.setCash(balance);
            responseVo.setBonus(BigDecimal.ZERO);
            responseVo.setUsedPromo(BigDecimal.ZERO);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            String betId = betResultIdempotentViolationException.getBetId();
            responseVo.setTransactionId(VendorService.getTransactionId(betId));
            responseVo.setCurrency(vendorCurrencyCode);
            responseVo.setCash(betResultIdempotentViolationException.getBalance());
            responseVo.setBonus(BigDecimal.ZERO);
            responseVo.setUsedPromo(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
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

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_FROZEN);
            httpService.logError(httpRequestLog, disabledAgentPlayerException);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InvalidSignatureException invalidHashException) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);
            httpService.logError(httpRequestLog, invalidHashException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (DisabledVendorLineException | DisabledGameException betNotAllowedException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
            httpService.logError(httpRequestLog, betNotAllowedException);

        } catch (GameNotSupportedException disabledGameException) {
            responseVo.setResponseCode(ResponseCode.INVALID_GAME);
            httpService.logError(httpRequestLog, disabledGameException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

            //operator response with invalid http status checking
            if (invalidOperatorResponseException.getOperatorStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            } else {
                responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            }

            httpService.logError(httpRequestLog, invalidOperatorResponseException);

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

    private void emitRawBetDetail(GameSession gameSession, BetDto dto, String gaBetId, String body) {
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
                    .eventKind(EventKind.PLACE_BET)
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

    private void doValidation(BetDto dto) throws InvalidRequestException, InvalidPlayerException {

        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }

    private void doVerification(HttpRequestLog request, BetDto dto, GameSession gameSession) throws
            AuthenticationException, InvalidPlayerException, CredentialNotFoundException,
            InvalidSignatureException, DisabledVendorLineException, DisabledAgentPlayerException,
            DisabledGameException, GameNotSupportedException {

        // 1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUserId());

        // 2. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 3. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);
    }
}
