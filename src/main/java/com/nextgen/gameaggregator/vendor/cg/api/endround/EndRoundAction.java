package com.nextgen.gameaggregator.vendor.cg.api.endround;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cg.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import com.nextgen.gameaggregator.vendor.cg.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
@RequestMapping(path = EndPoints.PATH)
public class EndRoundAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;

    @Autowired
    public EndRoundAction(HttpService httpService,
                          GameSessionService gameSessionService,
                          WalletService walletService,
                          VendorService vendorService,
                          VendorLineService vendorLineService,
                          UnsettledBetService unsettledBetService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(EndPoints.END_ROUND)
    public String endRound(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo endRoundVo = new ResponseVo();
        CommonDto dto = new CommonDto();
        try {
            //convert body into dto
            dto = HttpService.convertQueryStringToDto(httpRequestLog, CommonDto.class);
            dto.setData(VendorService.urlDecode(dto.getData()));

            //basic validation
            this.doValidation(dto);

            String decryptedData = vendorService.decryptData(dto.getData(), dto.getChannelId());//we get the json here
            httpRequestLog.setRequestBody(decryptedData);
            EndRoundDto endRoundDto = HttpService.convertJsonToDto(decryptedData, EndRoundDto.class);

            //search for game session
            GameSession gameSession;
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(endRoundDto.getAccountId(), endRoundDto.getGameType());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(endRoundDto.getGameType(), gameSession);
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(endRoundDto.getAccountId());
                gameSessionService.updateByVendorGameCode(gameSession, endRoundDto.getGameId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }
            //basic verification
            this.doVerification(endRoundDto, gameSession);

            //make a ResultType for settle process indicator
            ResultType resultType = vendorService.calculateResultType(BigDecimal.ZERO, endRoundDto.getWinAmount(), endRoundDto.getJackpotAmount(), false);

            //process
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, endRoundDto, resultType, vendorService, httpRequestLog);

            //set values
            endRoundVo.setChannelId(endRoundDto.getChannelId());
            endRoundVo.setAccountId(endRoundDto.getAccountId());
            endRoundVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            endRoundVo.setCurrency(endRoundDto.getCurrency());
            endRoundVo.setErrorCode(ResponseCodes.SUCCESS);
            endRoundVo.setReturnTime(VendorService.returnTime());
        } catch (GameNotSupportedException gameNotSupportedException) {
            endRoundVo.setErrorCode(ResponseCodes.GAMETYPE_ERROR);
            httpService.logError(httpRequestLog, gameNotSupportedException);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            endRoundVo.setErrorCode(ResponseCodes.CURRENCY_NOT_SUPPORTED);
            httpService.logError(httpRequestLog, currencyNotSupportedException);
        } catch (BetNotFoundException betNotFoundException) {
            endRoundVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_TRANSACTION);
            httpService.logError(httpRequestLog, betNotFoundException);
        } catch (InvalidVendorLineException invalidVendorLineException) {
            endRoundVo.setErrorCode(ResponseCodes.CHANNEL_ID_ERROR);
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            endRoundVo.setErrorCode(ResponseCodes.SEAMLESS_MTCODE_REPEAT);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
        } catch (DateTimeParseException dateTimeParseException) {
            endRoundVo.setErrorCode(ResponseCodes.SEAMLESS_TIME_FORMAT_ERROR);
            httpService.logError(httpRequestLog, dateTimeParseException);
        } catch (InvalidRequestException invalidRequestException) {
            endRoundVo.setErrorCode(ResponseCodes.SEAMLESS_INPUT_ERROR);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (Exception e) {
            endRoundVo.setErrorCode(ResponseCodes.UNKNOWN_ERROR);
        } finally {
            try {
                String jsonString = new Gson().toJson(endRoundVo);
                endRoundVo.setEncrypt(vendorService.encryptResponse(jsonString, dto.getChannelId())); //encrypt the whole vo include error
                httpService.end(httpRequestLog, endRoundVo);
            } catch (CredentialNotFoundException e) {
                httpService.logError(httpRequestLog, e);
            }
        }
        return endRoundVo.getEncrypt();
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        //basic validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(EndRoundDto dto, GameSession gameSession) throws CurrencyNotSupportedException, InvalidPlayerException, InvalidVendorLineException, CredentialNotFoundException, GameNotSupportedException {
        //check channel id
        String channelId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CHANNEL_ID);
        ValidationUtils.isEquals(channelId, dto.getChannelId(), InvalidVendorLineException::new);

    }
}
