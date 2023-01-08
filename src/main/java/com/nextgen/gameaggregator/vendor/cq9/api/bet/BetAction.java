package com.nextgen.gameaggregator.vendor.cq9.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
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

@RestController
@RequestMapping(path = EndPoints.PATH, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
@Slf4j
public class BetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = EndPoints.BET)
    public ResponseVo<CommonVo> bet(HttpServletRequest request) {
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
            BetDto betDto = HttpService.convertQueryStringToDtoUrlDecode(body, BetDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(betDto);
            ValidationUtils.validateLength(betDto.getAccount(), 3, 36, InvalidPlayerException::new);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betDto.getSession());

            // Throw exception if received username differs from game session
            if (!gameSession.getVendorPlayerUsername().equals(betDto.getAccount())) {
                throw new InvalidPlayerException();
            }

            // 5. Send bet request to Operator and check if player has enough balance
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);

            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(betEvent);

            commonVo.setBalance(betEvent.getLastBalance());
            commonVo.setCurrency(gameSession.getCurrencyCode());

            responseVo.setData(commonVo);

        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);

        } catch (AuthenticationException authenticationException) {
            statusVo.setCode(ResponseCodes.SERVER_ERROR);

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
