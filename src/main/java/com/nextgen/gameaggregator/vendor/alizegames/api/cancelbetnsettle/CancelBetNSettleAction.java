package com.nextgen.gameaggregator.vendor.alizegames.api.cancelbetnsettle;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.RawBetRefundLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.alizegames.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.alizegames.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.alizegames.service.VendorService;
import com.nextgen.gameaggregator.vendor.alizegames.vo.DataVo;
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
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = Endpoints.CANCEL_BET_N_SETTLE)
    public ResponseVo<DataVo> action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo<DataVo> responseVo = new ResponseVo<DataVo>();
        DataVo data = new DataVo();
        String traceId = httpRequestLog.getId();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CancelBetNSettleDto dto = HttpService.convertJsonToDto(body, CancelBetNSettleDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUsername());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 5. Process rollback
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService);

            // 6. Set response data
            data.setUsername(dto.getUsername());
            data.setBalance(balance);
            data.setCurrency(gameSession.getVendorCurrencyCode());
            data.setTimestamp(System.currentTimeMillis());
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setData(data);
        
        } catch (JsonProcessingException jsonProcessingException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (RecordNotFoundException recordNotFoundException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            RawBetRefundLog rawBetRefundLog = betRefundIdempotentViolationException.getBetRefundLog();
            responseVo.setResponseCode(ResponseCode.SUCCESS);

        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (Exception exception) { // any other exception encountered
            httpService.logError(httpRequestLog, exception);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
      
    }

     private void doValidation(CancelBetNSettleDto dto) throws InvalidRequestException {
            // General validation
            ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, CancelBetNSettleDto dto, GameSession gameSession) throws
            InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException, AuthenticationException, 
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException {

            validationService.validateEligibleBet(gameSession, dto.getUsername());
    }
}
