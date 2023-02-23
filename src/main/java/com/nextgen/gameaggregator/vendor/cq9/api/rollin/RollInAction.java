package com.nextgen.gameaggregator.vendor.cq9.api.rollin;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
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

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RollInAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @PostMapping(path = EndPoints.ROLLIN, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    private ResponseVo<CommonVo> rollIn(HttpServletRequest request) {
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
            RollInDto rollInDto = HttpService.convertQueryStringToDtoUrlDecode(body, RollInDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(rollInDto, wToken);

            // 2. Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(rollInDto.getAccount());
            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(rollInDto.getGamecode(), vendorPlayer.getVendorId());
            BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(rollInDto.getRoundId(), vendorGame.getId(), vendorPlayer.getId());

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betHistory.getGameSessionToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(rollInDto, gameSession, wToken, betHistory.getBetAmount());

            // 5. Process win data
            BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, rollInDto, body);

            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));

            // Construct VO data
            CommonVo commonVo = new CommonVo();
            commonVo.setBalance(betResultEvent.getLastBalance());
            commonVo.setCurrency(gameSession.getCurrencyCode());

            responseVo.setData(commonVo);
        } catch (AuthenticationException authenticationException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (BetNotFoundException betNotFoundException) {
            statusVo.setCode(ResponseCodes.TRANSACTION_RECORD_NOT_FOUND);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);

        } catch (DateTimeParseException dateTimeParseException) {
            statusVo.setCode(ResponseCodes.TIME_FORMAT_ERROR);

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            httpRequestLog.setErrorMessage(duplicateExternalTransactionIdException.getMessage());

        } catch (GameNotSupportedException gameNotSupportedException) {
            statusVo.setCode(ResponseCodes.GAME_ACTION_ERROR);

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

    private void doValidation(RollInDto dto, String wToken) throws InvalidRequestException, InvalidPlayerException, DateTimeParseException {
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(dto);

        // Validation with custom exception
        ValidationUtils.validateLength(dto.getAccount(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(dto.getGamehall(), Credentials.GAME_HALL, InvalidRequestException::new);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        formatter.parse(dto.getEventTime());

        // 5. Validate win amount
        switch (dto.getGametype()) {
            case "fish":
            case "arcade":
                if (dto.getWin().compareTo(BigDecimal.ZERO) < 0) throw new InvalidRequestException();
                break;
            default:
                break;
        }
    }

    private void doVerification(RollInDto dto, GameSession gameSession, String wToken, BigDecimal rolloutAmount) throws InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidVendorLineException, InvalidRequestException {
        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAccount(), InvalidPlayerException::new);

        // 2. Verify received game id is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGamecode(), AuthenticationException::new);

        // 3. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 4. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);

        // 5. Validate rollin amount
        switch (dto.getGametype()) {
            case "fish":
            case "arcade":
                if (dto.getAmount().compareTo(rolloutAmount.subtract(dto.getBet()).add(dto.getWin())) != 0)
                    throw new InvalidRequestException();
                break;
            case "table":
            case "live":
                if (dto.getAmount().compareTo(rolloutAmount.add(dto.getWin()).subtract(dto.getRake())) != 0)
                    throw new InvalidRequestException();
                break;
            default:
                break;
        }
    }
}
