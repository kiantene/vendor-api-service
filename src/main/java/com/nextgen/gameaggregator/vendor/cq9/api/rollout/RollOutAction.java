package com.nextgen.gameaggregator.vendor.cq9.api.rollout;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletServiceImpl;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RollOutAction {

    private final HttpService httpService;
    private final WalletRequestService walletRequestService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final ValidationService validationService;
    private final OperatorWalletService operatorWalletService;


    public RollOutAction(HttpService httpService,
                         WalletRequestService walletRequestService,
                         GameSessionService gameSessionService,
                         VendorLineService vendorLineService,
                         ValidationService validationService,
                         OperatorWalletServiceImpl operatorWalletService
    ) {

        this.httpService = httpService;
        this.walletRequestService = walletRequestService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.validationService = validationService;
        this.operatorWalletService = operatorWalletService;
    }

    private void dataMapper(WalletRequest walletRequest, RollOutDto dto, GameSession gameSession) {

        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setVendorPlayerUsername(dto.getAccount());
        walletRequest.setExternalTransactionId(dto.getMtcode());
        walletRequest.setRoundId(dto.getRoundid());
        walletRequest.setVendorGameCode(dto.getGamecode());
        walletRequest.setTimestamp(dto.getTimestamp());
        walletRequest.setToken(dto.getSession());
        walletRequest.setVendorBetId(dto.getRoundid());
        walletRequest.setTransferAmount(dto.getAmount());
    }

    @PostMapping(path = EndPoints.ROLLOUT, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseVo<CommonVo> rollOut(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        String wToken = request.getHeader("wtoken");

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        String errorMessage = "";

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            RollOutDto rollOutDto = HttpService.convertQueryStringToDtoUrlDecode(body, RollOutDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(rollOutDto, wToken);

            // add request idempotent check
            httpService.isDuplicateRequest(rollOutDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(rollOutDto.getSession());

            this.dataMapper(walletRequest, rollOutDto, gameSession);

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(walletRequest, gameSession, wToken);

            walletRequest = operatorWalletService.betDebit(walletRequest);

            CommonVo commonVo = new CommonVo();
            commonVo.setBalance(walletRequest.getBalanceAfter());
            commonVo.setCurrency(walletRequest.getCurrencyCode());
            responseVo.setData(commonVo);

        } catch (DuplicateRequestException duplicateRequestException) {
            statusVo.setCode(ResponseCodes.SUCCESS); // vendor requested to return success
            errorMessage = duplicateRequestException.getMessage();

        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            errorMessage = invalidRequestException.getMessage();

        } catch (AuthenticationException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException authenticationException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
            errorMessage = authenticationException.getMessage();

        } catch (DisabledGameException disabledGameException) {
            statusVo.setCode(ResponseCodes.GAME_ACTION_ERROR);
            errorMessage = disabledGameException.getMessage();

        } catch (DateTimeParseException dateTimeParseException) {
            statusVo.setCode(ResponseCodes.TIME_FORMAT_ERROR);
            errorMessage = dateTimeParseException.getMessage();

        } catch (InternalServerException | InvalidOperatorResponseException internalServerException) {
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            errorMessage = internalServerException.getMessage();

        } catch (InsufficientBalanceException insufficientBalanceException) {
            statusVo.setCode(ResponseCodes.INSUFFICIENT_BALANCE);
            errorMessage = insufficientBalanceException.getMessage();

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            errorMessage = exception.getMessage();

        } finally {
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));
            if (StringUtils.hasText(errorMessage)) {
                walletRequest.setErrorMessage(errorMessage);
            }
            walletRequestService.end(walletRequest, httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(RollOutDto rollOutDto, String wToken) throws InvalidRequestException, InvalidPlayerException {
        if (wToken == null) throw new InvalidRequestException("wToken is missing");

        // General validation
        ValidationUtils.validateRequest(rollOutDto);

        // Validation with custom exception
        ValidationUtils.validateLength(rollOutDto.getAccount(), 3, 20, InvalidPlayerException::new);

        DateTimeFormatter.ISO_DATE_TIME.parse(rollOutDto.getEventTime());
    }

    private void doVerification(WalletRequest walletRequest, GameSession gameSession, String wToken) throws
            AuthenticationException, InvalidPlayerException, DisabledAgentPlayerException, DisabledGameException, InternalServerException {

        try {
            // 1. Retrieve vendor line credentials and secretKey for verify API Token
            String walletToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.WALLET_TOKEN);

            // 2. Validate request Wallet Token
            ValidationUtils.isEquals(walletToken, wToken, AuthenticationException::new);

            // 3. Verify received username is the same from game session
            ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), walletRequest.getVendorPlayerUsername(), InvalidPlayerException::new);

            // 4. Verify received game id is the same from game session
            // comparison for game session value will always be using  AuthenticationException
            ValidationUtils.isEquals(gameSession.getVendorGameCode(), walletRequest.getVendorGameCode(), AuthenticationException::new);

            validationService.validateEligibleBet(gameSession, walletRequest.getVendorPlayerUsername());
        } catch (CredentialNotFoundException |
                 DisabledVendorLineException exception) {

            throw new InternalServerException(exception.getClass() + " : " + exception.getMessage());
        }
    }
}
