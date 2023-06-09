package com.nextgen.gameaggregator.vendor.alizegames.api.cancelbetnsettle;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.alizegames.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.alizegames.service.VendorService;
import com.nextgen.gameaggregator.vendor.alizegames.vo.ResponseVo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = Endpoints.PATH)
@Slf4j
public class CancelBetNSettleAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = Endpoints.CANCEL_BET_N_SETTLE)
    public ResponseVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CancelBetNSettleVo responseVo = new CancelBetNSettleVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CancelBetNSettleDto dto = HttpService.convertJsonToDto(body, CancelBetNSettleDto.class);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService);
            responseVo.setError(0);
            responseVo.setMessage("Success Operation");
            responseVo.setBalance(balance);
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setUsername(dto.getUsername());
            responseVo.setTimestamp(System.currentTimeMillis());
        
        } catch (JsonProcessingException jsonProcessingException) {
            jsonProcessingException.getMessage();
        } catch (AuthenticationException authenticationException) {
            authenticationException.getMessage();
        } catch (RecordNotFoundException recordNotFoundException) {
            recordNotFoundException.getMessage();
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            invalidAgentApiCredentialException.getMessage();
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            invalidOperatorResponseException.getMessage();
        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            betRefundIdempotentViolationException.getMessage();
        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            couchbaseDataIntegrityException.getMessage();
        } catch (BetNotFoundException betNotFoundException) {

        } catch (Exception exception) { // any other exception encountered
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
      
    }
}
