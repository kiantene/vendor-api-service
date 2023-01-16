package com.nextgen.gameaggregator.vendor.pragmaticplay.api.jackpot;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.*;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class JackpotAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = Endpoints.JACKPOT)
    public ResponseVo jackpot(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        JackpotVo responseVo = new JackpotVo();
        String traceId = httpRequestLog.getTraceId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            JackpotDto dto = HttpService.convertQueryStringToDto(body, JackpotDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);
            ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
            ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);

            // TODO: validate gameId with gameSession

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());
            // Throw exception if received username differs from game session
            if (!gameSession.getVendorPlayerUsername().equals(dto.getUserId())) {
                throw new InvalidPlayerException();
            }

            // 3. Retrieve vendor line credentials and secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
            VendorService.verifyHash(body, secretKey);

            // 5. Send win result to Operator
            BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, dto, body);

            // Emit event for additional asynchronous processingx
            EventDispatcherSystem.emitAsync(betResultEvent);

            responseVo.setTransactionId(traceId);
            responseVo.setCurrency(gameSession.getVendorGameCode());
            responseVo.setCash(betResultEvent.getLastBalance());
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, duplicateExternalTransactionIdException);

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
