package com.nextgen.gameaggregator.vendor.pragmaticplay.api.endround;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.event.EndRoundEvent;
import com.nextgen.gameaggregator.event.EventDispatcherSystem;
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
import java.util.UUID;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
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

    @PostMapping(path = Endpoints.END_ROUND)
    public ResponseVo endRound(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        EndRoundVo responseVo = new EndRoundVo();
        String traceId = UUID.randomUUID().toString();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            EndRoundDto dto = HttpService.convertQueryStringToDto(body, EndRoundDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);
            ValidationUtils.validateVendorUsername(dto.getUserId());
            ValidationUtils.validateEquals(dto.getProviderId(), Credentials.PROVIDER_ID);

            // TODO: validate gameId

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Retrieve vendor line credentials and secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
            VendorService.validateHash(body, secretKey);

            // 5. Retrieve the bet transaction
            BetHistory betHistory = betHistoryService.getBetTransaction(dto.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());

            // 6. Retrieve the latest wallet balance from Operator
            // TODO: performance tuning, may cache the last balance from Result and use that
            //  last balance to return to vendor, instead of making another call to Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emit(new EndRoundEvent(betHistory));

            responseVo.setCash(balance);
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setError(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (AuthenticationException authenticationException) {
            responseVo.setError(ResponseCodes.AUTHENTICATION_ERROR);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setError(ResponseCodes.INVALID_HASH);

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            httpRequestLog.setErrorMessage(betNotFoundException.getMessage());

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpRequestLog.setErrorMessage(HttpService.getStackTrace(exception));

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            if (!responseVo.getError().equals(ResponseCodes.SUCCESS)) {
                httpRequestLog.setStatus(HttpService.ERROR);
            }
            httpRequestLog.setEndTime(System.currentTimeMillis());
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }

        return responseVo;
    }
}
