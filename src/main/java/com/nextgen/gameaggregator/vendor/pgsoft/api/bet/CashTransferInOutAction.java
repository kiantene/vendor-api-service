package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;

@RestController
@RequestScope
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class CashTransferInOutAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;

    @Autowired
    private LoggingService loggingService;

    @PostMapping(path = Endpoints.BET)
    public ResponseVo<CashTransferInOutVo> betRequest(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo<CashTransferInOutVo> parentResponseVo = new ResponseVo<>();
        String traceId = httpRequestLog.getId();
        CashTransferInOutVo responseVo = new CashTransferInOutVo();
        String vendorCurrencyCode = "";

        try {
            CashTransferInOutDto dto = HttpService.convertQueryStringToDto(httpRequestLog, CashTransferInOutDto.class);
            vendorCurrencyCode = dto.getCurrencyCode();

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            loggingService.logStart();
            GameSession gameSession = gameSessionService.verifyToken(dto.getOperatorPlayerSession());
            loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ gameSessionService.verifyToken(" + dto.getOperatorPlayerSession() + ")", gameSession.getVendorPlayerUsername(), dto.getRoundId());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Process full bet data
            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), true);

            // 5. check is settledBet is exists
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);
            parentResponseVo.setData(responseVo);
            responseVo.setBalanceAmount(balance);
            responseVo.setCurrencyCode(vendorCurrencyCode);
            responseVo.setUpdatedTime(dto.getVendorSettleTime());

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            parentResponseVo.setErrorCode(ResponseCodes.PLAYER_OPERATION_IN_PROGRESS);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.PLAYER_OPERATION_IN_PROGRESS));
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            parentResponseVo.setData(responseVo);
            responseVo.setUpdatedTime(betResultIdempotentViolationException.getVendorSettleTime());
            responseVo.setBalanceAmount(betResultIdempotentViolationException.getBalance());
            responseVo.setCurrencyCode(vendorCurrencyCode);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidRequestException invalidRequestException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (AuthenticationException authenticationException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_PLAYER_SESSION_1300));
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            parentResponseVo.setErrorCode(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET));
            parentResponseVo.setData(null);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            parentResponseVo.setErrorCode(ResponseCodes.BET_FAILED);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.BET_FAILED));
            httpService.logError(httpRequestLog, currencyNotSupportedException);

        } catch (BetNotFoundException betNotFoundException) {
            parentResponseVo.setErrorCode(ResponseCodes.NO_BET_EXISTS);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.NO_BET_EXISTS));
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //SC_INSUFFICIENT_FUNDS
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                parentResponseVo.setErrorCode(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET);
                parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET));
                parentResponseVo.setData(null);

            } else {
                parentResponseVo.setErrorCode(ResponseCodes.OPERATION_FAILED);
                parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.OPERATION_FAILED));

            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidPlayerException invalidPlayerException) {
            parentResponseVo.setErrorCode(ResponseCodes.PLAYER_DOES_NOT_EXIST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.PLAYER_DOES_NOT_EXIST));
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_OPERATOR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_OPERATOR));
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);

        } catch (InvalidSignatureException invalidSignatureException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));
            httpService.logError(httpRequestLog, invalidSignatureException);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));
            httpService.logError(httpRequestLog, credentialNotFoundException);

        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            parentResponseVo.setErrorCode(ResponseCodes.OPERATION_FAILED);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.OPERATION_FAILED));
            httpService.logError(httpRequestLog, mergedBetDataIntegrityException);

        } catch (GameNotSupportedException gameNotSupportedException) {
            parentResponseVo.setErrorCode(ResponseCodes.GAME_DOES_NOT_EXIST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.GAME_DOES_NOT_EXIST));
            httpService.logError(httpRequestLog, gameNotSupportedException);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_PLAYER_SESSION_1300));
            httpService.logError(httpRequestLog, disabledAgentPlayerException);

        } catch (DisabledGameException disabledGameException) {
            parentResponseVo.setErrorCode(ResponseCodes.GAME_DOES_NOT_EXIST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.GAME_DOES_NOT_EXIST));
            httpService.logError(httpRequestLog, disabledGameException);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_OPERATOR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_OPERATOR));
            httpService.logError(httpRequestLog, disabledVendorLineException);

        } catch (BetFailedException betFailedException) {
            parentResponseVo.setErrorCode(ResponseCodes.BET_FAILED_3073);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.BET_FAILED_3073));
            httpService.logError(httpRequestLog, betFailedException);

        } catch (Exception exception) {
            parentResponseVo.setErrorCode(ResponseCodes.OPERATION_FAILED);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.OPERATION_FAILED));
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, parentResponseVo);
        }

        return parentResponseVo;
    }

    private void doValidation(CashTransferInOutDto dto) throws InvalidRequestException, InvalidPlayerException, BetFailedException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getPlayerName(), 3, 20, InvalidPlayerException::new);

        // Vendor Acceptance Test for AMB PGS
        if (dto.getWinAmount().subtract(dto.getBetAmount()).compareTo(dto.getTransferAmount()) != 0) {
            throw new BetFailedException();
        }
    }

    private void doVerification(HttpRequestLog request, CashTransferInOutDto dto, GameSession gameSession) throws
            InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidSignatureException,
            CurrencyNotSupportedException, GameNotSupportedException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, InterruptedException {

        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getPlayerName());

        // GA-119 PGSoft may enter game with different session
        // 2. Verify received game id is the same from game session
        // ValidationUtils.isEquals(rawGameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);
        loggingService.logStart();
        VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(dto.getGameId(), gameSession.getVendorId());
        loggingService.logProcessTimeTempLog("PROCESS 1 SECOND LOG ｜ vendorGameService.getByVendorGameCodeAndVendorId(" + dto.getGameId() + "," + gameSession.getVendorId() + ")", gameSession.getVendorPlayerUsername(), dto.getRoundId());

        //update session games while player is using session that is not matched with the game which played.
        if (vendorGame.getId() != gameSession.getVendorGameId()) {
            gameSession.setVendorGameId(vendorGame.getId());
            gameSession.setVendorGameCode(vendorGame.getVendorGameCode());
            gameSession.setGameCode(vendorGame.getCode());
            gameSession.setGameCategoryId(vendorGame.getGameCategory().getId());
            gameSessionService.updateSession(gameSession);
        }

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
    }
}
