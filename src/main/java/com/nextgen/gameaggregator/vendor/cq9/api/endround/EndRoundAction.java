package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.WinType;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
            List<EndRoundDataDto> endRoundDataDtoList = new Gson().fromJson(endRoundDto.getData(), new TypeToken<List<EndRoundDataDto>>() {
            }.getType());

            // 1. Validate request parameters from vendor
            this.doValidation(endRoundDto, endRoundDataDtoList, wToken);

            // 2. Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(endRoundDto.getAccount());
            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(endRoundDto.getGamecode(), vendorPlayer.getVendorId());
            BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(endRoundDto.getRoundid(), vendorGame.getId(), vendorPlayer.getId());

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betHistory.getGameSessionToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(endRoundDto, gameSession, wToken);

            // 5. Process win data
            WinDataDto winDataDto = new ObjectMapper().convertValue(endRoundDto, WinDataDto.class);
            winDataDto.setExternalTransactionId(endRoundDataDtoList.get(0).getMtcode());
            winDataDto.setAmount(endRoundDataDtoList.get(0).getAmount());
            winDataDto.setTimestamp(endRoundDataDtoList.get(0).getTimestamp());
            winDataDto.setWinType(this.getWinType(endRoundDto, endRoundDataDtoList.get(0).getAmount()));
            BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDataDto, body);

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

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
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

        } catch (ParseException parseException) {
            statusVo.setCode(ResponseCodes.TIME_FORMAT_ERROR);

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

    private void doValidation(EndRoundDto dto, List<EndRoundDataDto> endRoundDataDtoList, String wToken) throws InvalidRequestException, InvalidPlayerException, ParseException {
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(endRoundDataDtoList);

        // Validation with custom exception
        ValidationUtils.validateLength(dto.getAccount(), 3, 20, InvalidPlayerException::new);
        new SimpleDateFormat(dto.getCreateTime()).parse(Formats.DATE_TIME_FORMAT);
        new SimpleDateFormat(endRoundDataDtoList.get(0).getEventtime()).parse(Formats.DATE_TIME_FORMAT);
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

    private WinType getWinType(EndRoundDto endRoundDto, BigDecimal amount) {
        WinType winType;
        if (endRoundDto.getJackpot() != null) {
            winType = WinType.JACKPOT;
        } else if (endRoundDto.getBonus() != null) {
            winType = WinType.WIN;
        } else if (endRoundDto.getLuckydraw() != null) {
            winType = WinType.WIN;
        } else if (endRoundDto.getFreegame() != null) {
            winType = WinType.WIN;
        } else {
            winType = (amount.compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
        }

        return winType;
    }
}
