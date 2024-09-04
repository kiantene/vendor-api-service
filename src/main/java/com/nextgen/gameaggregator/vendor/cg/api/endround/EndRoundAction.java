package com.nextgen.gameaggregator.vendor.cg.api.endround;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cg.constant.Format;
import com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import com.nextgen.gameaggregator.vendor.cg.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
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
    public ResponseVo endRound(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo endRoundVo = new ResponseVo();
        try {
            //convert to dto
            EndRoundDto dto = HttpService.convertQueryStringToDtoUrlDecode(httpRequestLog, EndRoundDto.class);

            //basic validation
            this.doValidation(dto);

            //search for game session
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAccountId());

            //basic verification
            this.doVerification(dto, gameSession);

            //make a ResultType for settle process indicator
            ResultType resultType = vendorService.calculateResultType(BigDecimal.ZERO, dto.getWinAmount(), dto.getJackpotAmount(), false);

            //process
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            //set values
            endRoundVo.setChannelId(dto.getChannelId());
            endRoundVo.setAccountId(dto.getAccountId());
            endRoundVo.setBalance(balance);
            endRoundVo.setCurrency(dto.getCurrency());
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
        } catch (AuthenticationException authenticationException) {
            endRoundVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_PLAYER);
            httpService.logError(httpRequestLog, authenticationException);
        } catch (DateTimeParseException dateTimeParseException) {
            endRoundVo.setErrorCode(ResponseCodes.SEAMLESS_TIME_FORMAT_ERROR);
            httpService.logError(httpRequestLog, dateTimeParseException);
        } catch (InvalidRequestException invalidRequestException) {
            endRoundVo.setErrorCode(ResponseCodes.SEAMLESS_INPUT_ERROR);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (Exception e) {
            endRoundVo.setErrorCode(ResponseCodes.UNKNOWN_ERROR);
        } finally {
            httpService.end(httpRequestLog, endRoundVo);
        }
        return endRoundVo;
    }

    private void doValidation(EndRoundDto dto) throws InvalidRequestException {
        //basic validation
        ValidationUtils.validateRequest(dto);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Format.DATE_TIME_FORMAT);
        formatter.parse(dto.getEventTime());
    }

    private void doVerification(EndRoundDto dto, GameSession gameSession) throws CurrencyNotSupportedException, InvalidPlayerException, InvalidVendorLineException, CredentialNotFoundException, GameNotSupportedException {
        //check channel id
        String channelId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CHANNEL_ID);
        ValidationUtils.isEquals(channelId, dto.getChannelId(), InvalidVendorLineException::new);

        //check session gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);
        //check session currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }
}
