package com.nextgen.gameaggregator.vendor.dotconnections.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.SettledBet;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.WagerTypes;
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
import java.util.Objects;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelWagerAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private WalletAdjustmentService walletAdjustmentService;
    @Autowired
    private SettledBetService settledBetService;

    @PostMapping(path = EndPoints.CANCEL_WAGER)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getId();
        GameSession gameSession = null;
        CancelWagerDto dto = null;

        try {

            // Get request body
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            dto = HttpService.convertJsonToDto(body, CancelWagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Get last game session
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getBrandUid());

            // Verify data
            this.doVerification(dto, gameSession);

            /*
            Note: cancel (deduct) end wager record
             wagerType = 1: return/increase the amount to the player wallet (Mainly use rollback for this case)
             wagerType = 2: deduct/reduce the amount to the player's wallet. (This is to cancel settled bet but we don't do that)
             */
            if (dto.getWagerType().equals(WagerTypes.CANCEL_END_WAGER)) {
                throw new BetNotFoundException();
            }

            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService);

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());

            // Set Vendor player username + Balance + Currency
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);
            responseDataVo.setBalance(balance);

        } catch (AuthenticationException | InvalidVendorLineException | InvalidSignatureException signErrorException) {
            responseVo.setCode(ResponseCodes.SIGN_ERROR);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setCode(ResponseCodes.CURRENCY_NOT_SUPPORT);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setCode(ResponseCodes.PLAYER_NOT_EXIST);

        } catch (DisabledGameException disabledGameException) {
            responseVo.setCode(ResponseCodes.GAME_ID_NOT_EXIST);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setCode(ResponseCodes.REQUEST_PARAM_ERROR);

        } catch (InvalidProviderException invalidProviderException) {
            responseVo.setCode(ResponseCodes.INVALID_PROVIDER);

        } catch (BetNotFoundException betRecordNotExistException) {
            // get current balance
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BET_RECORD_NOT_EXIST);

        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            // get current balance
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);

        } catch (DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 CredentialNotFoundException |
                 InvalidAgentApiCredentialException |
                 JsonProcessingException |
                 RecordNotFoundException systemErrorException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, systemErrorException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            if (betResultIdempotentViolationException.getStatus().equals(BetStatus.REFUNDED.code)) {
                // if bet already refunded
                responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
                responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);

            } else if (Objects.equals(betResultIdempotentViolationException.getStatus(), BetStatus.SETTLED.code)) {
                // if bet already settled
                responseVo = this.doAdjustment(traceId, gameSession, dto, httpRequestLog);

            } else {
                // if found the bet other in settled status (cancel / unsettle / settled)
                responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
                httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            }

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (Exception exception) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);

        }

        return responseVo;

    }

    private void doValidation(CancelWagerDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

    }

    private void doVerification(CancelWagerDto dto, GameSession gameSession)
            throws
            InvalidPlayerException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidVendorLineException,
            CredentialNotFoundException,
            AuthenticationException,
            InvalidSignatureException,
            InvalidRequestException,
            InvalidProviderException {

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

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }

    private ResponseVo doAdjustment(String traceId, GameSession gameSession, CancelWagerDto dto, HttpRequestLog httpRequestLog) {

        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        try {
            // get the settled bet for adjustment
            SettledBet settledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(dto.getWagerId(), dto.getRoundId(), gameSession.getVendorId(), gameSession.getVendorPlayerId());

            dto.setAdjustmentAmount(settledBet.getBetAmount());
            BigDecimal balance = walletAdjustmentService.processAdjustment(traceId, gameSession, dto, httpRequestLog);

            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(balance);
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BALANCE_INSUFFICIENT);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);

        } catch (BetNotFoundException | SettledBetNotFoundException recordNotFoundException) {
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BET_RECORD_NOT_EXIST);

        } catch (BetAdjustmentIdempotentViolationException betAdjustmentIdempotentViolationException) {
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);

        } catch (InvalidAgentApiCredentialException | InvalidOperatorResponseException systemErrorException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, systemErrorException);

        } catch (Exception exception) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);

        }

        return responseVo;

    }

}
