package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.eventing.events.ResultBetEvent;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
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
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class EndRoundAction {
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
    @Autowired
    private Environment environment;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.END_ROUND, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseVo<CommonVo> endRound(HttpServletRequest request) {
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
            EndRoundDto endRoundDto = HttpService.convertQueryStringToDtoUrlDecode(body, EndRoundDto.class);
            ValidationUtils.validateRequest(endRoundDto);
            List<EndRoundDataDto> endRoundDataDtoList = HttpService.convertJsonToDto(endRoundDto.getData(), new TypeReference<List<EndRoundDataDto>>() {});

            // 1. Validate request parameters from vendor
            this.doValidation(endRoundDto, endRoundDataDtoList, wToken);

            // 2. Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(endRoundDto.getAccount());
            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(endRoundDto.getGamecode(), vendorPlayer.getVendorId());
            UnsettledBet unsettledBet = betHistoryService.getRawUnsettledBetByBetIdAndRoundIdAndGameIdAndPlayerId(endRoundDto.getVendorBetId(),
                    endRoundDto.getRoundId(), vendorGame.getId(), vendorPlayer.getId());

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(unsettledBet.getGameSessionToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(endRoundDto, gameSession, wToken);

            // 5. Process extra endRoundDto bet data
            this.doProcessExtraEndRoundDto(endRoundDataDtoList, endRoundDto, unsettledBet);

            // 6. Process result settle data
            // temporary code to ensure when commit to stg branch will still use old code for new changes
            CommonVo commonVo = new CommonVo();

            if(environment.getProperty("spring.couchbase.userName") == "stg"){
                //if env = stg will use old code
                SettledBetEvent settledBetEvent = walletService.processResultSettle(traceId, gameSession, endRoundDto, body);
                commonVo.setBalance(settledBetEvent.getLastBalance());

            } else {
                //else use new code
                ResultBetEvent resultBetEvent = walletService.processBetResult(traceId, gameSession, endRoundDto, ResultType.WIN, vendorService, body);
                commonVo.setBalance(resultBetEvent.getLastBalance());
            }

            // Construct VO data
            commonVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setData(commonVo);

        } catch (AuthenticationException authenticationException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (BetNotFoundException betNotFoundException) {
            statusVo.setCode(ResponseCodes.TRANSACTION_RECORD_NOT_FOUND);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);

        } catch (DateTimeParseException dateTimeParseException) {
            statusVo.setCode(ResponseCodes.TIME_FORMAT_ERROR);

        } catch (GameNotSupportedException gameNotSupportedException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);

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

        } catch (JsonProcessingException jsonProcessingException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);

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

    private void doValidation(EndRoundDto dto, List<EndRoundDataDto> endRoundDataDtoList, String wToken) throws InvalidRequestException, InvalidPlayerException, DateTimeParseException {
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(endRoundDataDtoList.get(0));

        // Validation with custom exception
        ValidationUtils.validateLength(dto.getAccount(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.isEquals(dto.getGamehall(), Credentials.GAME_HALL, InvalidRequestException::new);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        formatter.parse(dto.getCreateTime());
        formatter.parse(endRoundDataDtoList.get(0).getEventtime());
        dto.setExternalTransactionId(endRoundDataDtoList.get(0).getMtcode());
    }

    private void doVerification(EndRoundDto dto, GameSession gameSession, String wToken) throws InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidVendorLineException {
        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAccount(), InvalidPlayerException::new);

        // 2. Verify received game id is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGamecode(), AuthenticationException::new);

        // 3. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 4. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);
    }

    private ResultType getWinType(EndRoundDto endRoundDto, BigDecimal amount) {
        ResultType resultType;
        if (endRoundDto.getJackpot() != null) {
            resultType = ResultType.JACKPOT;
        } else {
            resultType = (amount.compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.LOSE;
        }

        return resultType;
    }

    private void doProcessExtraEndRoundDto(List<EndRoundDataDto> endRoundDataDtoList, EndRoundDto dto, UnsettledBet unsettledBet){

        Instant instant = Instant.parse(endRoundDataDtoList.get(0).getEventtime());
        Long resultTime = instant.toEpochMilli();

        dto.setResultTime(resultTime);
        dto.setVendorSettleTime(dto.getResultTime());

        dto.setWinAmount(endRoundDataDtoList.get(0).getAmount());
        dto.setEffectiveTurnover(unsettledBet.getBetAmount());
        dto.setWinLoss(dto.getWinAmount().subtract(unsettledBet.getBetAmount()));
        dto.setResultType(this.getWinType(dto, dto.getWinAmount()));
    }
}
