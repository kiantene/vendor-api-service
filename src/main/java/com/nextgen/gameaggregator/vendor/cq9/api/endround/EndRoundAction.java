package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.api.result.ResultService;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    private ResultService resultService;

    @PostMapping(path = EndPoints.END_ROUND, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseVo<CommonVo> endRound(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        CommonVo commonVo = new CommonVo();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            EndRoundDto endRoundDto = HttpService.convertQueryStringToDtoUrlDecode(body, EndRoundDto.class);
            List<EndRoundDataDto> endRoundDataDtoList = new Gson().fromJson(endRoundDto.getData(), new TypeToken<List<EndRoundDataDto>>(){}.getType());
            WinDataDto winDataDto = new ObjectMapper().convertValue(endRoundDto, WinDataDto.class);
            winDataDto.setExternalTransactionId(endRoundDataDtoList.get(0).getMtcode());
            winDataDto.setAmount(endRoundDataDtoList.get(0).getAmount());
            winDataDto.setTimestamp(endRoundDataDtoList.get(0).getTimestamp());

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(endRoundDto);
            ValidationUtils.validateLength(endRoundDto.getAccount(), 3, 36, InvalidPlayerException::new);

            // 2. Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(endRoundDto.getAccount());
            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(endRoundDto.getGamecode(), vendorPlayer.getVendorId());
            BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(endRoundDto.getRoundid(), vendorGame.getId(), vendorPlayer.getId());

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betHistory.getGameSessionToken());

            // 4. Process win data
            BetResultEvent betResultEvent = resultService.process(traceId, gameSession, winDataDto, body);

            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(new EndRoundEvent(betHistory));

            commonVo.setBalance(walletService.getBalance(traceId, gameSession));
            commonVo.setCurrency(gameSession.getCurrencyCode());

            responseVo.setData(commonVo);

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
}
