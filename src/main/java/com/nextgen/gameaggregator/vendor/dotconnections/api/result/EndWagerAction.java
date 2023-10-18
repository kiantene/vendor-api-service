package com.nextgen.gameaggregator.vendor.dotconnections.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.UnsettledBet;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
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
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class EndWagerAction {

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
    private UnsettledBetService unsettledBetService;

    @PostMapping(path = EndPoints.END_WAGER)
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
            EndWagerDto dto = HttpService.convertJsonToDto(body, EndWagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Get last game session
            // TODO: To handle duplicate bet exception (vendor identify duplicate by round_id and wager_id)
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getBrandUid());

            // Verify data
            this.doVerification(dto, gameSession);

            // Process bet
            BigDecimal balance = this.processEndRoundBet(traceId, dto, gameSession, httpRequestLog);

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(balance);

            // Set data for response vo
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);

        } catch (AuthenticationException | InvalidVendorLineException | InvalidSignatureException signErrorException) {
            responseVo.setCode(ResponseCodes.SIGN_ERROR);
            httpService.logError(httpRequestLog, signErrorException);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setCode(ResponseCodes.CURRENCY_NOT_SUPPORT);
            httpService.logError(httpRequestLog, currencyNotSupportedException);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setCode(ResponseCodes.PLAYER_NOT_EXIST);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (DisabledGameException disabledGameException) {
            responseVo.setCode(ResponseCodes.GAME_ID_NOT_EXIST);
            httpService.logError(httpRequestLog, disabledGameException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            // get current balance
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BALANCE_INSUFFICIENT);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (BetNotFoundException betNotFoundException) {
            // get current balance
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BET_RECORD_NOT_EXIST);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            // get current balance
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(betResultIdempotentViolationException.getBalance());
            responseVo.setData(responseDataVo);
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setCode(ResponseCodes.REQUEST_PARAM_ERROR);
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

    private void doValidation(EndWagerDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(EndWagerDto dto, GameSession gameSession)
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

        // Verify currency + game code
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }

    private BigDecimal processEndRoundBet(String traceId, EndWagerDto dto, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws
            BetNotFoundException,
            InvalidAgentApiCredentialException,
            VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException,
            MergedBetDataIntegrityException,
            InsufficientBalanceException,
            TransactionStillProcessingException,
            InvalidOperatorResponseException {

        /*
        List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(dto.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());

        if (unsettledBetList.isEmpty()) {
            throw new BetNotFoundException("Cannot find round Id: " + dto.getRoundId());
        }

         */

        // Set as BET_WIN / BET_LOST because
        // vendor's BO has wager and endWager records
        // endWager's bet id is different from wager's bet id
        ResultType resultType = (dto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.BET_WIN : ResultType.BET_LOSE;

        return walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

    }
}
