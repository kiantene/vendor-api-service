package com.nextgen.gameaggregator.vendor.cq9.api.authenticate;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping(path = EndPoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class CheckPlayerAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @GetMapping(path = EndPoints.AUTHENTICATE)
    public ResponseVo<Boolean> authenticate(@PathVariable("account") String account, HttpServletRequest request) {
        // Construct Vo
        ResponseVo<Boolean> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        Boolean isValid = false;
        String traceId = UUID.randomUUID().toString();

        try {
            /*
            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(checkPlayerDto);

            // 2. Verify session token
            // Need to retrieve line credentials from game session in order to validate hash
            // If Token has been tampered, then AuthenticationException will be thrown
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Retrieve vendor line credentials and secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
            VendorService.validateHash(body, secretKey);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Emit event for additional asynchronous processing
            // eventDispatcher.emit(getClass(), body);
            */
            isValid = true;

            responseVo.setData(isValid);

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpRequestLog.setErrorMessage(HttpService.getStackTrace(exception));
        } finally {
            if (!statusVo.getCode().equals("0")) {
                httpRequestLog.setStatus(HttpService.ERROR);
            }
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));
            httpRequestLog.setEndTime(System.currentTimeMillis());
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }

        return responseVo;
    }
}
