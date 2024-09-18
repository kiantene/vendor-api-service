package com.nextgen.gameaggregator.vendor.cq9.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
@Slf4j
public class BetAction {
    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final ValidationService validationService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final VendorService vendorService;

    @Autowired
    public BetAction(GameSessionService gameSessionService,
                     HttpService httpService,
                     ValidationService validationService,
                     VendorLineService vendorLineService,
                     WalletService walletService,
                     VendorService vendorService) {
        this.gameSessionService = gameSessionService;
        this.httpService = httpService;
        this.validationService = validationService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.BET)
    public ResponseVo<CommonVo> bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();
        String wToken = request.getHeader("wtoken");

        // Construct VO
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        CommonVo commonVo = new CommonVo();
        responseVo.setStatus(statusVo);
        String vendorCurrencyCode = "";

        try {
            // Convert original request body into dto
            BetDto betDto = HttpService.convertQueryStringToDtoUrlDecode(httpRequestLog, BetDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto, wToken);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betDto.getSession());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betDto.getGameId(), gameSession);

            vendorCurrencyCode = gameSession.getVendorCurrencyCode();

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession, wToken);

            // 4. Process unsettle data
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);

            // Construct VO
            commonVo.setBalance(betEvent.getLastBalance());
            commonVo.setCurrency(vendorCurrencyCode);
            responseVo.setData(commonVo);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            commonVo.setBalance(betResultIdempotentViolationException.getBalance());
            commonVo.setCurrency(vendorCurrencyCode);
            responseVo.setData(commonVo);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (AuthenticationException authenticationException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            httpService.logError(httpRequestLog, credentialNotFoundException);

        } catch (DateTimeParseException dateTimeParseException) {
            statusVo.setCode(ResponseCodes.TIME_FORMAT_ERROR);
            httpService.logError(httpRequestLog, dateTimeParseException);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, disabledAgentPlayerException);

        } catch (DisabledGameException disabledGameException) {
            statusVo.setCode(ResponseCodes.GAME_ACTION_ERROR);
            httpService.logError(httpRequestLog, disabledGameException);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, disabledVendorLineException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            statusVo.setCode(ResponseCodes.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidPlayerException invalidPlayerException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (InvalidVendorLineException invalidVendorLineException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, invalidVendorLineException);

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(BetDto betDto, String wToken) throws InvalidRequestException, InvalidPlayerException, DateTimeParseException {
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(betDto);

        // Validation with custom exception
        ValidationUtils.validateLength(betDto.getAccount(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(betDto.getGamehall(), Credentials.GAME_HALL, InvalidRequestException::new);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        formatter.parse(betDto.getEventTime());
    }

    private void doVerification(BetDto betDto, GameSession gameSession, String wToken) throws AuthenticationException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {
        // 1. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 2. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);

        // 3. Verify received game id is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), betDto.getGameId(), AuthenticationException::new);

        //4.. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, betDto.getAccount());
    }
}
