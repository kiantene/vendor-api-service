package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestScope
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
public class CashTransferInOutAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @PostMapping(path = Endpoints.BET)
    public ResponseVo<CashTransferInOutVo> betRequest(WebRequestWrapper request) {
        // Construct Vo
        ResponseVo<CashTransferInOutVo> parentResponseVo = new ResponseVo<>();
        CashTransferInOutVo responseVo = new CashTransferInOutVo();
        parentResponseVo.setData(responseVo);

        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        String traceId = UUID.randomUUID().toString();
        Long now = Instant.now().toEpochMilli();

        try {

            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert original request body into dto
            CashTransferInOutDto dto = HttpService.convertQueryStringToDto(body, CashTransferInOutDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getOperatorPlayerSession());
            // 4. Send bet request to Operator and check if player has enough balance
            BigDecimal balance = walletService.processBet(traceId, gameSession, dto);

            //* hardcoded response
            responseVo.setUpdatedTime(now);
            responseVo.setBalanceAmount(balance);
            responseVo.setCurrencyCode(gameSession.getCurrencyCode());

        } catch (InvalidRequestException invalidRequestException) {
            parentResponseVo.setError(ResponseCodes.INVALID_REQUEST);

        } catch (AuthenticationException authenticationException) {
            parentResponseVo.setError(ResponseCodes.INVALID_PLAYER_SESSION_1300);

        } finally {
            if (parentResponseVo.getError() != null) {
                httpRequestLog.setStatus(HttpService.ERROR);
            }
            httpRequestLog.setEndTime(System.currentTimeMillis());
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }

        return parentResponseVo;
    }

}
