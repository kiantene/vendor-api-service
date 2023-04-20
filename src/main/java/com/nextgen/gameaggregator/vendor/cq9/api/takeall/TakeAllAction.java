package com.nextgen.gameaggregator.vendor.cq9.api.takeall;

import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class TakeAllAction {

    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.TAKE_ALL, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseVo<CommonVo> takeAll(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();
        String wToken = request.getHeader("wtoken");

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            TakeAllDto takeAllDto = HttpService.convertQueryStringToDtoUrlDecode(body, TakeAllDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(takeAllDto, wToken);

            // 2. Verify session token
            RawGameSession rawGameSession = gameSessionService.verifyToken(takeAllDto.getSession());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(takeAllDto, rawGameSession, wToken);

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            BigDecimal walletBalance = walletService.getBalance(traceId, rawGameSession);
            takeAllDto.setAmount(walletBalance);
            BetEvent betEvent = walletService.processBet(traceId, rawGameSession, takeAllDto, body);

            // Construct VO
            CommonVo commonVo = new CommonVo();
            commonVo.setBalance(betEvent.getLastBalance());
            commonVo.setCurrency(rawGameSession.getVendorCurrencyCode());
            responseVo.setData(commonVo);

        } catch (AuthenticationException authenticationException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);

        } catch (DateTimeParseException dateTimeParseException) {
            statusVo.setCode(ResponseCodes.TIME_FORMAT_ERROR);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (DisabledGameException disabledGameException) {
            statusVo.setCode(ResponseCodes.GAME_ACTION_ERROR);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            statusVo.setCode(ResponseCodes.DUPLICATE_EXTERNAL_TRANSACTION_ID);
            httpRequestLog.setErrorMessage(duplicateExternalTransactionIdException.getMessage());

        } catch (InsufficientBalanceException insufficientBalanceException) {
            statusVo.setCode(ResponseCodes.INSUFFICIENT_BALANCE);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidPlayerException invalidPlayerException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (InvalidVendorLineException invalidVendorLineException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

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

    private void doValidation(TakeAllDto takeAllDto, String wToken) throws InvalidRequestException, InvalidPlayerException {
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(takeAllDto);

        // Validation with custom exception
        ValidationUtils.validateLength(takeAllDto.getAccount(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(takeAllDto.getGamehall(), Credentials.GAME_HALL, InvalidRequestException::new);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        formatter.parse(takeAllDto.getEventTime());
    }

    private void doVerification(TakeAllDto takeAllDto, RawGameSession rawGameSession, String wToken) throws AuthenticationException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {
        // 1. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(rawGameSession.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 2. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);

        // 3. Verify received username is the same from game session
        ValidationUtils.isEquals(rawGameSession.getVendorPlayerUsername(), takeAllDto.getAccount(), InvalidPlayerException::new);

        // 4. Verify received game id is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(rawGameSession.getVendorGameCode(), takeAllDto.getGameId(), AuthenticationException::new);

        // 5. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(rawGameSession.getVendorLineId());

        // 6. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(rawGameSession.getAgentPlayerId());

        // 7. Verify vendor game is active
        vendorGameService.verifyGameStatus(rawGameSession.getVendorGameId());
    }
}
