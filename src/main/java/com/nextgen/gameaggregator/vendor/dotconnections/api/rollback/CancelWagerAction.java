package com.nextgen.gameaggregator.vendor.dotconnections.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.*;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    @Autowired
    private UnsettledBetCachingService unsettledBetCachingService;
    @Autowired
    private CachingService cachingService;

    @PostMapping(path = EndPoints.CANCEL_WAGER)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getId();
        GameSession gameSession = null;
        CancelWagerDto dto = null;
        AtomicBoolean checkBetStatus = new AtomicBoolean(false);

        try {

            // Get request body
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            dto = HttpService.convertJsonToDto(body, CancelWagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Get last game session
            gameSession = this.getGameSession(traceId, dto, checkBetStatus);

            // Verify data
            this.doVerification(dto, gameSession);

            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);

            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);
            responseDataVo.setBalance(balance);

        } catch (InvalidSignatureException signErrorException) {
            responseVo.setCode(ResponseCodes.SIGN_ERROR);
            httpService.logError(httpRequestLog, signErrorException);

        } catch (AuthenticationException authenticationException) {
            responseVo.setCode(ResponseCodes.INVALID_BRAND_UID);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (CurrencyNotSupportedException | VendorCurrencyNotSupportException currencyNotSupportedException) {
            responseVo.setCode(ResponseCodes.CURRENCY_NOT_SUPPORT);
            httpService.logError(httpRequestLog, currencyNotSupportedException);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setCode(ResponseCodes.PLAYER_NOT_EXIST);
            httpService.logError(httpRequestLog, invalidPlayerException);

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

            vendorService.scheduleTempSessionTokenDeletion(dto.getBrandUid(), dto.getRoundId());

        } catch (InvalidProviderException invalidProviderException) {
            responseVo.setCode(ResponseCodes.INVALID_PROVIDER);
            httpService.logError(httpRequestLog, invalidProviderException);

        } catch (BetNotFoundException betRecordNotExistException) {
            // get current balance
            vendorService.errorResponseBetNotFound(dto, responseVo);
            responseVo.setCode(ResponseCodes.BET_RECORD_NOT_EXIST);
            httpService.logError(httpRequestLog, betRecordNotExistException);

        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            // get current balance
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);
            httpService.logError(httpRequestLog, betRefundIdempotentViolationException);

        } catch (CredentialNotFoundException |
                 InvalidAgentApiCredentialException |
                 JsonProcessingException |
                 RecordNotFoundException systemErrorException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, systemErrorException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            // if bet already refunded
            responseDataVo.setBrandUid(dto.getBrandUid());
            responseDataVo.setCurrency(dto.getCurrency());
            responseDataVo.setBalance(betResultIdempotentViolationException.getBalance());
            responseVo.setData(responseDataVo);
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

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
            CredentialNotFoundException,
            InvalidSignatureException,
            InvalidProviderException {

        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        String toVerifySign = VendorService.getSign(brandId + dto.getWagerId() + apiKey);

        String providerCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROVIDER_CODE);

        // Verify signature
        VendorService.isSameSignature(dto.getSign(), toVerifySign);

        // Verify if is valid player
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getBrandUid(), InvalidPlayerException::new);

        // Verify provider
        if (!dto.getProvider().equals(providerCode)) {
            throw new InvalidProviderException();
        }

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }

    private GameSession getGameSession(String traceId, CancelWagerDto dto, AtomicBoolean checkBetStatus)
            throws InvalidPlayerException,
            VendorCurrencyNotSupportException,
            GameNotSupportedException,
            AuthenticationException,
            BetResultIdempotentViolationException,
            BetNotFoundException {
        GameSession gameSession;

        String token = cachingService.cacheableTokenByRoundIdAndVendorPlayerUsernameToRedis(dto.getBrandUid(), dto.roundId, null);

        try {
            if (token == null) {
                throw new AuthenticationException("Token not found");
            } else {
                gameSession = gameSessionService.verifyToken(token);
            }
        } catch (AuthenticationException e) {
            VendorGame vendorGame = vendorService.verifyBetStatusAndGetVendorGameId(dto.getBrandUid(), dto.getRoundId(), dto.getRollbackId(), checkBetStatus);
            gameSession = gameSessionService.generateNewSessionToken(dto.getBrandUid());
            gameSessionService.updateByVendorGameCode(gameSession, vendorGame.getVendorGameCode());
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(traceId);
            gameSession.setVendorToken(traceId);
        }
        return gameSession;
    }
}
