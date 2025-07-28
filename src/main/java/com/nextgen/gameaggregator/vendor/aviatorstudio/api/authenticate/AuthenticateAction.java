package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class AuthenticateAction {

    private final HttpService httpService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;

    public AuthenticateAction(HttpService httpService, VendorService vendorService, GameSessionService gameSessionService, WalletService walletService) {
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    @GetMapping(path = EndPoints.AUTHENTICATE)
    public CommonVo account(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();

        try {
            String traceId = httpRequestLog.getId();
            String queryString = request.getQueryString();
            String jwtAuth = request.getHeader("Authorization");

            AuthenticateDto dto = HttpService.convertQueryStringToDto(queryString, AuthenticateDto.class);
            dto.setAuthorization(jwtAuth);

            //Add request header log
            httpRequestLog.setRequestBody("Request Body: \n" + queryString + "\n\nRequest Header: \n" + vendorService.getHeaders(request));

            VendorService.doValidation(dto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(VendorService.jwtGetUserId(jwtAuth));

            this.doVerification(jwtAuth, dto.getCurrency(), gameSession);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            responseVo.setResponseSuccess(balance, gameSession.getVendorPlayerId().toString(), gameSession.getVendorPlayerUsername());

        } catch (Exception e) {
            this.handleException(e, responseVo, httpRequestLog);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doVerification(String jwtAuth, String currency, GameSession gameSession) throws
            AuthenticationException,
            InvalidRequestException,
            CredentialNotFoundException {
        //Verify JWT
        vendorService.verifyJWT(jwtAuth, gameSession.getVendorLineId(), gameSession.getVendorPlayerUsername());

        //Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), currency, InvalidRequestException::new);

    }

    @ExceptionHandler({AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonVo responseVo, HttpRequestLog httpRequestLog) {

        if (e instanceof AuthenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTH_ERROR);
        } else {
            responseVo.setResponseCode(ResponseCode.SERVER_ERROR);
        }
        httpService.logError(httpRequestLog, e);
    }
}
