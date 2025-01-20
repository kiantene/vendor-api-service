package com.nextgen.gameaggregator.vendor.koolbet.api.cancelSessionBet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import com.nextgen.gameaggregator.vendor.koolbet.api.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelSessionBet {

    private final HttpService httpService;

    private final GameSessionService gameSessionService;

    private final WalletService walletService;


    private final VendorService vendorService;


    @Autowired
    public CancelSessionBet(HttpService httpService, GameSessionService gameSessionService, WalletService walletService
            , VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;

        this.vendorService = vendorService;

    }

    @PostMapping(path = EndPoints.CANCEL_SESSION_BET)
    public CommonVo rollback(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CommonVo responseVo = new CommonVo();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CancelSessionBetDto dto = HttpService.convertJsonToDto(body, CancelSessionBetDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession;
            try { //this check only verify if it's null, not status = 0
                gameSession = gameSessionService.verifyToken(dto.getToken());
            } catch (AuthenticationException authenticationException) { //if session expired
                gameSession = gameSessionService.generateNewSessionToken(dto.getUserId()); //generate new token
                gameSessionService.updateByVendorGameCode(gameSession, String.valueOf(dto.getGame()));
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // 5. Process rollback
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);

            //Set Response Data
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setUsername(traceId);
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setBalance(balance.doubleValue());

        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(CancelSessionBetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelSessionBetDto dto, GameSession gameSession)
            throws CredentialNotFoundException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CredentialNotFoundException::new);

    }
}
