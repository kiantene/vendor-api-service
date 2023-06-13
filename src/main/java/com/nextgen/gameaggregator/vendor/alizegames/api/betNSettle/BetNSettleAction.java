package com.nextgen.gameaggregator.vendor.alizegames.api.betNSettle;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.RawBetResultLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
public class BetNSettleAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = Endpoints.BET_N_SETTLE)
    public ResponseVo<DataVo> betResult(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo<DataVo> responseVo = new ResponseVo<DataVo>();
        DataVo data = new DataVo();
        String traceId = httpRequestLog.getId();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BetNSettleDto dto = HttpService.convertJsonToDto(body, BetNSettleDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUsername());
            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 5. Send win result to Operator
            Integer isBet = 1;
            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), isBet);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            // 6. Set response data
            data.setUsername(dto.getUsername());
            data.setBalance(balance);
            data.setCurrency(dto.getCurrency());
            data.setTimestamp(System.currentTimeMillis());
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setData(data);

        } catch (BetResultIdempotentViolationException idempotentViolationException) {
            // Return original result when idempotent
            RawBetResultLog rawBetResultLog = idempotentViolationException.getBetResultLog();
            data.setUsername(String.valueOf(rawBetResultLog.getVendorPlayerId()));
            data.setBalance(rawBetResultLog.getBalance());
            data.setCurrency(rawBetResultLog.getVendorCurrencyCode());
            data.setTimestamp(System.currentTimeMillis());
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setData(data);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            
        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidAgentApiCredentialException InvalidAgentApiCredentialException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpRequestLog.setErrorMessage(couchbaseDataIntegrityException.getMessage());

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpRequestLog.setErrorMessage(betNotFoundException.getMessage());
            
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            responseVo.setResponseCode(ResponseCode.ERROR);
        
        } catch (DisabledGameException disabledGameException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, exception);
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }

    private void doValidation(BetNSettleDto dto) throws InvalidRequestException {
            // General validation
            ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, BetNSettleDto dto, GameSession gameSession) throws
            InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException, AuthenticationException, 
            DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException {

            validationService.validateEligibleBet(gameSession, dto.getUsername());
    }
}
