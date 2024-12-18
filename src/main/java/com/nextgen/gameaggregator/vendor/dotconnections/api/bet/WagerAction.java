package com.nextgen.gameaggregator.vendor.dotconnections.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RawBetRefundLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.exception.DuplicateBetRecordException;
import com.nextgen.gameaggregator.vendor.dotconnections.exception.InvalidProviderException;
import com.nextgen.gameaggregator.vendor.dotconnections.service.VendorService;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class WagerAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private BetRefundLogService betRefundLogService;

    @PostMapping(path = EndPoints.WAGER)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getId();
        GameSession gameSession = null;

        try {

            // Get request body
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            WagerDto dto = HttpService.convertJsonToDto(body, WagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(VendorService.revertToUUID(dto.getToken()));

            // Verify data
            this.doVerification(dto, gameSession);

            // Check if bet transaction has been refunded before
            this.checkBetRefundLog(gameSession, dto);

            // Process bet
            // Vendor identify duplicate bet by round_id and wager_id
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);
            BigDecimal balance = betEvent.getLastBalance();

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(balance);

            // Set data for response vo
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);

        } catch (InvalidVendorLineException | InvalidSignatureException signErrorException) {
            responseVo.setCode(ResponseCodes.SIGN_ERROR);
            httpService.logError(httpRequestLog, signErrorException);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setCode(ResponseCodes.CURRENCY_NOT_SUPPORT);
            httpService.logError(httpRequestLog, currencyNotSupportedException);

        } catch (AuthenticationException authenticationException) {
            responseVo.setCode(ResponseCodes.PLAYER_NOT_EXIST);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (GameNotSupportedException gameNotSupportedException) {
            if (gameSession.getVendorId() == 18) {
                //this response is for relax gaming
                responseVo.setCode(ResponseCodes.NOT_LOGGED_IN);
            } else {
                responseVo.setCode(ResponseCodes.REQUEST_PARAM_ERROR);
            }
            httpService.logError(httpRequestLog, gameNotSupportedException);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setCode(ResponseCodes.NOT_LOGGED_IN);
            httpService.logError(httpRequestLog, invalidPlayerException);
        } catch (DisabledGameException disabledGameException) {
            responseVo.setCode(ResponseCodes.GAME_ID_NOT_EXIST);
            httpService.logError(httpRequestLog, disabledGameException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            // get current balance
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BALANCE_INSUFFICIENT);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            // get current balance
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(betResultIdempotentViolationException.getBalance());
            responseVo.setData(responseDataVo);
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (DuplicateBetRecordException duplicateBetRecordException) {
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);
            httpService.logError(httpRequestLog, duplicateBetRecordException);

        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if (invalidRequestException.getValidation() != null) {
                responseVo.setCode(
                        invalidRequestException.getValidation()
                                .entrySet()
                                .stream()
                                .findFirst()
                                .map(Map.Entry::getValue) // get the value of the first element
                                .orElse(ResponseCodes.REQUEST_PARAM_ERROR)
                );

            } else {
                responseVo.setCode(ResponseCodes.REQUEST_PARAM_ERROR);

            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (InvalidProviderException invalidProviderException) {
            responseVo.setCode(ResponseCodes.INVALID_PROVIDER);
            httpService.logError(httpRequestLog, invalidProviderException);

        } catch (DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 CredentialNotFoundException |
                 InvalidAgentApiCredentialException |
                 JsonProcessingException |
                 TransactionStillProcessingException systemErrorException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, systemErrorException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus().equals(com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
                responseVo.setCode(ResponseCodes.BET_RECORD_NOT_EXIST);

            } else {
                responseVo.setCode(ResponseCodes.SYSTEM_ERROR);

            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (Exception exception) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;

    }

    private void doValidation(WagerDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(WagerDto dto, GameSession gameSession)
            throws
            InvalidPlayerException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidVendorLineException,
            CredentialNotFoundException,
            AuthenticationException,
            InvalidRequestException,
            InvalidSignatureException,
            InvalidProviderException,
            GameNotSupportedException {

        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        String toVerifySign = VendorService.getSign(brandId + dto.getWagerId() + apiKey);

        String providerCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROVIDER_CODE);

        // Verify signature
        VendorService.isSameSignature(dto.getSign(), toVerifySign);

        // Verify provider
        if (!dto.getProvider().equals(providerCode)) {
            throw new InvalidProviderException();
        }

        // Verify if is valid player
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getBrandUid(), InvalidPlayerException::new);

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getBrandUid());

        // Verify currency + game code
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);
    }

    private void checkBetRefundLog(GameSession gameSession, WagerDto dto) throws DuplicateBetRecordException {
        RawBetRefundLog rawBetRefundLog = betRefundLogService.checkExists(
                gameSession.getVendorPlayerId().toString(),
                gameSession.getVendorGameId().toString(),
                dto.getExternalTransactionId()
        );

        if (rawBetRefundLog != null) {
            throw new DuplicateBetRecordException();
        }
    }
}
