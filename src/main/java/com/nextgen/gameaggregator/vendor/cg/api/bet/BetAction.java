package com.nextgen.gameaggregator.vendor.cg.api.bet;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cg.constant.Format;
import com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cg.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import com.nextgen.gameaggregator.vendor.cg.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final ValidationService validationService;
    private final VendorService vendorService;

    @Autowired
    public BetAction(HttpService httpService,
                     GameSessionService gameSessionService,
                     WalletService walletService,
                     VendorLineService vendorLineService,
                     ValidationService validationService,
                     VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.validationService = validationService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.BET)
    public String transaction(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo betVo = new ResponseVo();
        CommonDto dto = new CommonDto();
        try {
            //convert body into dto
            dto = HttpService.convertQueryStringToDto(httpRequestLog, CommonDto.class);
            dto.setData(VendorService.urlDecode(dto.getData()));

            //basic validation
            this.doValidation(dto);

            String decryptedData = vendorService.decryptData(dto.getData(), dto.getChannelId());//we get the json here
            httpRequestLog.setRequestBody(decryptedData);
            BetDto betDto = HttpService.convertJsonToDto(decryptedData, BetDto.class);

            //get game session
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getAccountId());

            //basic verification
            this.doVerification(betDto, gameSession);

            //process
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);

            //set values
            betVo.setChannelId(betDto.getChannelId());
            betVo.setAccountId(betDto.getAccountId());
            betVo.setBalance(betEvent.getLastBalance());
            betVo.setCurrency(betDto.getCurrency());
            betVo.setErrorCode(ResponseCodes.SUCCESS);
            betVo.setReturnTime(VendorService.returnTime());
        } catch (InsufficientBalanceException insufficientBalanceException) {
            betVo.setErrorCode(ResponseCodes.SEAMLESS_INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, insufficientBalanceException);
        } catch (GameNotSupportedException gameNotSupportedException) {
            betVo.setErrorCode(ResponseCodes.GAMETYPE_ERROR);
            httpService.logError(httpRequestLog, gameNotSupportedException);
        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            betVo.setErrorCode(ResponseCodes.SEAMLESS_MTCODE_REPEAT);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            betVo.setErrorCode(ResponseCodes.CURRENCY_NOT_SUPPORTED);
            httpService.logError(httpRequestLog, currencyNotSupportedException);
        } catch (InvalidVendorLineException invalidVendorLineException) {
            betVo.setErrorCode(ResponseCodes.CHANNEL_ID_ERROR);
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (AuthenticationException authenticationException) {
            betVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_PLAYER);
            httpService.logError(httpRequestLog, authenticationException);
        } catch (DateTimeParseException dateTimeParseException) {
            betVo.setErrorCode(ResponseCodes.SEAMLESS_TIME_FORMAT_ERROR);
            httpService.logError(httpRequestLog, dateTimeParseException);
        } catch (InvalidRequestException invalidRequestException) {
            betVo.setErrorCode(ResponseCodes.SEAMLESS_INPUT_ERROR);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (Exception e) {
            betVo.setErrorCode(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            try {
                String jsonString = new Gson().toJson(betVo);
                betVo.setEncrypt(vendorService.encryptResponse(jsonString, dto.getChannelId())); //encrypt the whole vo include error
                httpService.end(httpRequestLog, betVo);
            } catch (CredentialNotFoundException e) {
                httpService.logError(httpRequestLog, e);
            }
        }
        return betVo.getEncrypt();
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        //basic validation
        ValidationUtils.validateRequest(dto);

    }

    private void doVerification(BetDto dto, GameSession gameSession) throws CurrencyNotSupportedException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, GameNotSupportedException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException {
        String channelId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CHANNEL_ID);
        ValidationUtils.isEquals(channelId, dto.getChannelId(), InvalidVendorLineException::new);

        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Format.DATE_TIME_FORMAT);
        formatter.parse(dto.getEventTime());

        //check session gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);
        //check session currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        //check player id is same as session id
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAccountId(), InvalidPlayerException::new);
    }
}
