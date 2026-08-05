package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextHolder;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.*;
import com.nextgen.gameaggregator.vendor.pgsoft.service.PGSoftPromoPayoutService;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestScope
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class CashTransferInOutAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final VendorService vendorService;
    private final ValidationService validationService;
    private final LoggingService loggingService;
    private final RequestIdempotentLogService requestIdempotentLogService;
    private final VendorGameCodeService vendorGameCodeService;
    private final PGSoftPromoPayoutService promoPayoutService;

    public CashTransferInOutAction(HttpService httpService,
                                   GameSessionService gameSessionService,
                                   WalletService walletService,
                                   VendorLineService vendorLineService,
                                   VendorGameService vendorGameService,
                                   VendorService vendorService,
                                   ValidationService validationService,
                                   LoggingService loggingService,
                                   RequestIdempotentLogService requestIdempotentLogService,
                                   VendorGameCodeService vendorGameCodeService,
                                   PGSoftPromoPayoutService promoPayoutService) {

        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
        this.validationService = validationService;
        this.loggingService = loggingService;
        this.requestIdempotentLogService = requestIdempotentLogService;
        this.vendorGameCodeService = vendorGameCodeService;
        this.promoPayoutService = promoPayoutService;
    }

    @PostMapping(path = Endpoints.BET)
    public ResponseEntity<ResponseVo<CashTransferInOutVo>> betRequest(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo<CashTransferInOutVo> parentResponseVo = new ResponseVo<>();
        String traceId = httpRequestLog.getId();
        CashTransferInOutVo responseVo = new CashTransferInOutVo();
        String vendorCurrencyCode = "";
        CashTransferInOutDto dto = new CashTransferInOutDto();
        boolean isRequestExists = false;

        try {
            dto = HttpService.convertQueryStringToDto(httpRequestLog, CashTransferInOutDto.class);

            vendorCurrencyCode = dto.getCurrencyCode();

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // TODO : catch new exception and error mapping to vendor
            if (promoPayoutService.isPromoPayout(dto)) {
                return promoPayoutService.doPromoPayout(dto, httpRequestLog);
            }

            // request idempotent checking.
            if (requestIdempotentLogService.checkExists(dto, dto.getPlayerName()) == null) {
                requestIdempotentLogService.create(dto, dto.getPlayerName());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 2. Verify session token
            String newToken = (dto.getOperatorPlayerSession() != null) ? dto.getOperatorPlayerSession() : traceId;
            GameSession gameSession = this.getGameSession(newToken, dto);
            //GA-10954: for temporary using for log agent id
            httpRequestLog.setAgentId(gameSession.getAgentId());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // 4. Process full bet data
            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), true);

            // 5. Is End Round then set config to send topic round ended info
            if (dto.isEndRound() == 1) {
                BetResultContextHolder.initialise()
                        .configure(config -> config.setSettleType(SettleType.ROUND));
                BetResultContext betResultContext = BetResultContextHolder.getBetResultContext();
                betResultContext.setRoundEnded(BetStatus.SETTLED.isValueOf(dto.getBetStatus().code));
            }

            // 6. check is settledBet is exists
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);
            parentResponseVo.setData(responseVo);
            responseVo.setBalanceAmount(balance.setScale(2, RoundingMode.DOWN));
            responseVo.setCurrencyCode(vendorCurrencyCode);
            responseVo.setUpdatedTime(dto.getUpdatedTime());
            responseVo.setRealTransferAmount(dto.getRealTransferAmount());

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            parentResponseVo.setError(ResponseCodes.PLAYER_OPERATION_IN_PROGRESS);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            parentResponseVo.setData(responseVo);
            responseVo.setRealTransferAmount(dto.getRealTransferAmount());
            responseVo.setUpdatedTime(dto.getUpdatedTime());
            responseVo.setBalanceAmount(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN));
            responseVo.setCurrencyCode(vendorCurrencyCode);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidRequestException | CredentialNotFoundException |
                 InvalidSignatureException invalidRequestException) {

            parentResponseVo.setError(dto.getRealTransferAmount() == null
                    ? ResponseCodes.INVALID_REAL_TRANSFER_AMOUNT
                    : ResponseCodes.INVALID_REQUEST);
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (GameTerminatedException | AuthenticationException gameException) {
            parentResponseVo.setError(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            httpService.logError(httpRequestLog, gameException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            parentResponseVo.setError(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET);
            parentResponseVo.setData(null);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            parentResponseVo.setError(ResponseCodes.BET_FAILED);
            httpService.logError(httpRequestLog, currencyNotSupportedException);

        } catch (BetNotFoundException | BetNotAllowedException betNotFoundException) {
            parentResponseVo.setError(ResponseCodes.NO_BET_EXISTS);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //SC_INSUFFICIENT_FUNDS
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                parentResponseVo.setError(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET);
                parentResponseVo.setData(null);

            } else {
                parentResponseVo.setError(ResponseCodes.OPERATION_FAILED);

            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (IllegalArgumentException illegalArgumentException) {
            parentResponseVo.setError(ResponseCodes.INVALID_REAL_TRANSFER_AMOUNT);
            httpService.logError(httpRequestLog, illegalArgumentException);

        } catch (InvalidPlayerException invalidPlayerException) {
            parentResponseVo.setError(ResponseCodes.PLAYER_DOES_NOT_EXIST);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (DisabledVendorLineException invalidAgentApiCredentialException) {
            parentResponseVo.setError(ResponseCodes.INVALID_OPERATOR);
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);

        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            parentResponseVo.setError(ResponseCodes.OPERATION_FAILED);
            httpService.logError(httpRequestLog, mergedBetDataIntegrityException);

        } catch (GameNotSupportedException gameNotSupportedException) {
            parentResponseVo.setError(ResponseCodes.GAME_DOES_NOT_EXIST);
            httpService.logError(httpRequestLog, gameNotSupportedException);

        } catch (DisabledGameException disabledGameException) {
            parentResponseVo.setError(ResponseCodes.BET_FAILED);
            httpService.logError(httpRequestLog, disabledGameException);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            parentResponseVo.setError(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            httpService.logError(httpRequestLog, disabledAgentPlayerException);

        } catch (BetFailedException | InvalidAgentApiCredentialException betFailedException) {
            parentResponseVo.setError(ResponseCodes.BET_FAILED_3073);
            httpService.logError(httpRequestLog, betFailedException);

        } catch (InvalidGameCategoryException ex) {
            parentResponseVo.setError(ResponseCodes.INVALID_REQUEST);
            httpService.logError(httpRequestLog, ex);

        } catch (Exception exception) {
            parentResponseVo.setError(ResponseCodes.OPERATION_FAILED);
            httpService.logError(httpRequestLog, exception);

        } finally {

            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getPlayerName());
            }

            // skip this logging if it is for promo payout
            // logs will be printed via promo payout engine instead
            if (!promoPayoutService.isPromoPayout(dto)) {
                httpService.end(httpRequestLog, parentResponseVo);
            }
        }

        return ResponseEntity.ok(parentResponseVo);
    }

    private GameSession getGameSession(String token, CashTransferInOutDto dto) throws
            InvalidPlayerException, GameNotSupportedException, VendorCurrencyNotSupportException {

        GameSession gameSession;
        try {
            gameSession = gameSessionService.verifyToken(token);
        } catch (AuthenticationException authenticationException) {
            gameSession = gameSessionService.generateNewSessionToken(dto.getPlayerName());
            gameSessionService.updateByVendorGameCode(gameSession, dto.getGameId());
            gameSessionService.updateByVendorCurrencyCode(gameSession, dto.getCurrencyCode());

            Integer defaultPlatformId = (dto.getPlatform() == Platforms.WEB) ? 2 : 1;
            gameSession.setLanguageId(vendorGameCodeService.getByTop1VendorGameId(gameSession.getVendorGameId()).getLanguageId());
            gameSession.setToken(token);
            gameSession.setVendorToken(token);
            gameSession.setPlatformId(defaultPlatformId);
        }
        return gameSession;
    }

    private void doValidation(CashTransferInOutDto dto) throws
            InvalidRequestException,
            InvalidPlayerException,
            BetNotAllowedException,
            BetFailedException,
            IllegalArgumentException, InvalidGameCategoryException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getPlayerName(), 3, 20, InvalidPlayerException::new);

        // Vendor Acceptance Test for AMB PGS
        if (dto.getWinAmount().subtract(dto.getBetAmount()).compareTo(dto.getTransferAmount()) != 0) {
            throw new BetFailedException();
        }

        //GA-12228 VAT Test,validate real transfer amount
        this.validateRealTransferAmount(dto.getTransferAmount(), dto.getRealTransferAmount(), dto.getCurrencyCode());

        //GA-12228: Disallow transaction type 400 from betting
        if (dto.getTransactionId().contains("-400-")) {
            throw new InvalidGameCategoryException("Transaction type 400 is not allowed");
        }
    }

    private void doVerification(CashTransferInOutDto dto, GameSession gameSession) throws
            InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidSignatureException,
            CurrencyNotSupportedException, GameNotSupportedException, DisabledAgentPlayerException, DisabledGameException,
            DisabledVendorLineException, GameTerminatedException {

        // GA-119 PGSoft may enter game with different session
        // 2. Verify received game id is the same from game session
        // ValidationUtils.isEquals(rawGameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);
        loggingService.logStart();
        VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(dto.getGameId(), gameSession.getVendorId());
        loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ vendorGameService.getByVendorGameCodeAndVendorId(" + dto.getGameId() + "," + gameSession.getVendorId() + ")", gameSession.getVendorPlayerUsername(), dto.getRoundId());

        //update session games while player is using session that is not matched with the game which played.
        Integer originalVendorGameId = gameSession.getVendorGameId();
        String originalVendorGameCode = gameSession.getVendorGameCode();
        String originalGameCode = gameSession.getGameCode();
        Integer originalGameCategoryId = gameSession.getGameCategoryId();
        boolean isUsingDifferentGame = !vendorGame.getId().equals(originalVendorGameId);
        if (isUsingDifferentGame) {
            gameSession.setVendorGameId(vendorGame.getId());
            gameSession.setVendorGameCode(vendorGame.getVendorGameCode());
            gameSession.setGameCode(vendorGame.getCode());
            gameSession.setGameCategoryId(vendorGame.getGameCategoryId());
        }

        boolean verificationPassed = false;
        try {
            //1. validate vendor username, agent vendor line, player status, and requested game status
            validationService.isBetAllowed(gameSession, dto.getPlayerName());

            // 3. Verify vendor currency code is the same from game session
            ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrencyCode(), CurrencyNotSupportedException::new);

            // 4. Retrieve vendor line credentials and secretKey to verify with raw request from vendor
            loggingService.logStart();
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
            loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ vendorLineService.getCredentialValueByName(" + gameSession.getVendorLineId() + "," + Credentials.SECRET_KEY + ")", gameSession.getVendorPlayerUsername(), dto.getRoundId());
            ValidationUtils.isEquals(secretKey, dto.getSecretKey(), InvalidSignatureException::new);

            // 5. Retrieve vendor line credentials and operatorToken to verify with raw request from vendor
            loggingService.logStart();
            String operatorToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_TOKEN);
            loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ vendorLineService.getCredentialValueByName(" + gameSession.getVendorLineId() + "," + Credentials.OPERATOR_TOKEN + ")", gameSession.getVendorPlayerUsername(), dto.getRoundId());
            ValidationUtils.isEquals(operatorToken, dto.getOperatorToken(), InvalidSignatureException::new);

            verificationPassed = true;
        } finally {
            if (!verificationPassed && isUsingDifferentGame) {
                gameSession.setVendorGameId(originalVendorGameId);
                gameSession.setVendorGameCode(originalVendorGameCode);
                gameSession.setGameCode(originalGameCode);
                gameSession.setGameCategoryId(originalGameCategoryId);
            }
        }

        //persist session game update after requested game validation passed.
        if (isUsingDifferentGame) {
            gameSessionService.updateSession(gameSession);
        }

    }

    private void validateRealTransferAmount(BigDecimal transferAmount, BigDecimal realTransferAmount, String currencyCode) throws IllegalArgumentException {

        // Check if the currency uses thousand-rate conversion
        if (CurrencyThousandRate.isThousandRate(currencyCode)) {
            transferAmount = transferAmount.multiply(BigDecimal.valueOf(1000));
        }

        // Compare the expected transfer amount with the real transfer amount
        if (realTransferAmount.compareTo(transferAmount) != 0) {
            // Throw an exception if the amounts do not match
            throw new IllegalArgumentException("Invalid real_transfer_amount: " + realTransferAmount);
        }

    }
}
