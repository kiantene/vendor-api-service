package com.nextgen.gameaggregator.vendor.dotconnections.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextHolder;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.entity.ga.*;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class EndWagerAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final CachingService cachingService;

    @Autowired
    public EndWagerAction(HttpService httpService,
                          GameSessionService gameSessionService,
                          VendorLineService vendorLineService,
                          WalletService walletService,
                          VendorService vendorService,
                          CachingService cachingService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.cachingService = cachingService;
    }

    @PostMapping(path = EndPoints.END_WAGER)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getId();
        GameSession gameSession = null;
        EndWagerDto dto = null;
        AtomicBoolean checkBetStatus = new AtomicBoolean(false);
        try {

            // Get request body
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            dto = HttpService.convertJsonToDto(body, EndWagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Get last game session
            gameSession = this.getGameSession(traceId, dto, checkBetStatus);

            // Verify data
            this.doVerification(dto, gameSession, checkBetStatus);

            BetResultContextHolder.initialise()
                    .configure(config -> config.setSettleType(SettleType.ROUND));
            BetResultContext betResultContext = BetResultContextHolder.getBetResultContext();
            betResultContext.setRoundEnded(BetStatus.SETTLED.isValueOf(dto.getBetStatus().code));

            // Process bet
            ResultType resultType = (dto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.BET_WIN : ResultType.BET_LOSE;
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(balance);

            // Set data for response vo
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);

            if (dto.getBetStatus().code.equals(BetStatus.SETTLED.code)) {
                vendorService.scheduleTempSessionTokenDeletion(dto.getBrandUid(), dto.getRoundId());
            }

        } catch (InvalidSignatureException signErrorException) {
            responseVo.setCode(ResponseCodes.SIGN_ERROR);
            httpService.logError(httpRequestLog, signErrorException);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            responseVo.setCode(ResponseCodes.CURRENCY_NOT_SUPPORT);
            httpService.logError(httpRequestLog, currencyNotSupportedException);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setCode(ResponseCodes.PLAYER_NOT_EXIST);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            // get current balance
            responseVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession);
            responseVo.setCode(ResponseCodes.BALANCE_INSUFFICIENT);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (BetNotFoundException betNotFoundException) {
            // get current balance
            vendorService.errorResponseBetNotFound(dto, responseVo);
            responseVo.setCode(ResponseCodes.BET_RECORD_NOT_EXIST);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            // get current balance
            responseDataVo.setBrandUid((dto.getBrandUid() == null) ? "" : dto.getBrandUid());
            responseDataVo.setCurrency(dto.getCurrency());
            responseDataVo.setBalance(betResultIdempotentViolationException.getBalance());
            responseVo.setData(responseDataVo);
            responseVo.setCode(ResponseCodes.BET_RECORD_DUPLICATE);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

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

        } catch (CredentialNotFoundException |
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

    private void doVerification(EndWagerDto dto, GameSession gameSession, AtomicBoolean checkBetStatus)
            throws
            InvalidPlayerException,
            CurrencyNotSupportedException,
            CredentialNotFoundException,
            InvalidSignatureException,
            BetResultIdempotentViolationException,
            BetNotFoundException,
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

        // Verify currency + game code
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        //GA-12099 Verify this round have place bet or not for VAT test (because this resultType is BET_WIN/BET_LOSE)
        if (!checkBetStatus.get()) {
            vendorService.verifyBetStatus(dto);
        }
    }

    private GameSession getGameSession(String traceId, EndWagerDto dto, AtomicBoolean checkBetStatus) throws BetNotFoundException, InvalidPlayerException, GameNotSupportedException, VendorCurrencyNotSupportException, BetResultIdempotentViolationException {

        GameSession gameSession;
        try {
            String token = cachingService.cacheableTokenByRoundIdAndVendorPlayerUsernameToRedis(dto.getBrandUid(), dto.roundId, null);

            if (token == null) {
                throw new AuthenticationException("Token not found");
            } else {
                gameSession = gameSessionService.verifyToken(token);

            }
        } catch (AuthenticationException e) {
            VendorGame vendorGame = vendorService.verifyBetStatusAndGetVendorGameId(dto.getBrandUid(),
                    dto.getRoundId(), dto.getExternalTransactionId(), checkBetStatus);
            gameSession = gameSessionService.generateNewSessionToken(dto.getBrandUid());
            gameSessionService.updateByVendorGameCode(gameSession, vendorGame.getVendorGameCode());
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(traceId);
            gameSession.setVendorToken(traceId);
        }
        return gameSession;
    }
}

