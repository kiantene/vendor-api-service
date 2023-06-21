package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.SettledBet;
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

    @PostMapping(path = Endpoints.BET)
    public ResponseVo<CashTransferInOutVo> betRequest(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo<CashTransferInOutVo> parentResponseVo = new ResponseVo<>();
        String traceId = httpRequestLog.getId();
        CashTransferInOutVo responseVo = new CashTransferInOutVo();
        parentResponseVo.setData(responseVo);
        String vendorCurrencyCode = "";

        try {
            String body = httpRequestLog.getRequestBody();
            CashTransferInOutDto dto = HttpService.convertQueryStringToDto(body, CashTransferInOutDto.class);
            vendorCurrencyCode = dto.getCurrencyCode();

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getOperatorPlayerSession());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Process full bet data
            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), true);

            // 5. check is settledBet is exists
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);
            responseVo.setBalanceAmount(balance);
            responseVo.setCurrencyCode(vendorCurrencyCode);
            responseVo.setUpdatedTime(dto.getVendorSettleTime());

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            parentResponseVo.setErrorCode(ResponseCodes.PLAYER_OPERATION_IN_PROGRESS);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.PLAYER_OPERATION_IN_PROGRESS));

        } catch (SettledBetIdempotentViolationException settledBetIdempotentViolationException) {
            SettledBet settledBet = settledBetIdempotentViolationException.getSettledBet();

            responseVo.setUpdatedTime(settledBet.getVendorSettleTime());
            responseVo.setBalanceAmount(settledBet.getPlayerBalance());
            responseVo.setCurrencyCode(vendorCurrencyCode);

        } catch (InvalidRequestException invalidRequestException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (AuthenticationException authenticationException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_PLAYER_SESSION_1300));

        } catch (InsufficientBalanceException insufficientBalanceException) {
            parentResponseVo.setErrorCode(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET));

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            parentResponseVo.setErrorCode(ResponseCodes.BET_FAILED);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.BET_FAILED));

        } catch (BetNotFoundException betNotFoundException) {
            parentResponseVo.setErrorCode(ResponseCodes.NO_BET_EXISTS);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.NO_BET_EXISTS));

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //SC_INSUFFICIENT_FUNDS
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                parentResponseVo.setErrorCode(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET);
                parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET));
            } else {
                parentResponseVo.setErrorCode(ResponseCodes.INTERNAL_SERVER_ERROR);
                parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INTERNAL_SERVER_ERROR));
            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidPlayerException invalidPlayerException) {
            parentResponseVo.setErrorCode(ResponseCodes.PLAYER_DOES_NOT_EXIST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.PLAYER_DOES_NOT_EXIST));

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_OPERATOR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_OPERATOR));

        } catch (InvalidSignatureException invalidSignatureException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (CredentialNotFoundException credentialNotFoundException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            parentResponseVo.setErrorCode(ResponseCodes.OPERATION_FAILED);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.OPERATION_FAILED));

        } catch (GameNotSupportedException gameNotSupportedException) {
            parentResponseVo.setErrorCode(ResponseCodes.GAME_DOES_NOT_EXIST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.GAME_DOES_NOT_EXIST));

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_PLAYER_SESSION_1300));

        } catch (DisabledGameException disabledGameException) {
            parentResponseVo.setErrorCode(ResponseCodes.GAME_DOES_NOT_EXIST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.GAME_DOES_NOT_EXIST));

        } catch (DisabledVendorLineException disabledVendorLineException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_OPERATOR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_OPERATOR));

        } catch (Exception exception) {
            parentResponseVo.setErrorCode(ResponseCodes.OPERATION_FAILED);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.OPERATION_FAILED));
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, parentResponseVo);
        }

        return parentResponseVo;
    }

    private void doValidation(CashTransferInOutDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getPlayerName(), 3, 20, InvalidPlayerException::new);
    }

    private void doVerification(HttpRequestLog request, CashTransferInOutDto dto, GameSession gameSession) throws
            InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidSignatureException,
            CurrencyNotSupportedException, GameNotSupportedException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException {

        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getPlayerName());


        // GA-119 PGSoft may enter game with different session
        // 2. Verify received game id is the same from game session
        // ValidationUtils.isEquals(rawGameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);
        vendorGameService.getByVendorGameCodeAndVendorId(dto.getGameId(), gameSession.getVendorId());

        // 3. Verify vendor currency code is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrencyCode(), CurrencyNotSupportedException::new);

        // 4. Retrieve vendor line credentials and secretKey to verify with raw request from vendor
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(secretKey, dto.getSecretKey(), InvalidSignatureException::new);

        // 5. Retrieve vendor line credentials and operatorToken to verify with raw request from vendor
        String operatorToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_TOKEN);
        ValidationUtils.isEquals(operatorToken, dto.getOperatorToken(), InvalidSignatureException::new);
    }
}
