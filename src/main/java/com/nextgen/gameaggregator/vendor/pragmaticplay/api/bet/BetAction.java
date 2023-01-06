package com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
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
public class BetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = Endpoints.BET)
    public ResponseVo betRequest(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        BetVo responseVo = new BetVo();
        String traceId = httpRequestLog.getTraceId();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            BetDto dto = HttpService.convertQueryStringToDto(body, BetDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);
            ValidationUtils.validateVendorUsername(dto.getUserId()); // TODO: to support throwing of custom exception

            ValidationUtils.validateEquals(dto.getProviderId(), Credentials.PROVIDER_ID);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            //TODO (by Alex), should check the credential line status based on game_sessions table's vendor_line_id and block if the credential line status is disable

            //TODO (by Alex), should check the agent player status based on game_sessions table's agent_player_id and block if the player status is disable

            //TODO (by Alex), validate game login token game Id match with bet request

            // Throw exception if received username differs from game session
            if (!gameSession.getVendorPlayerUsername().equals(dto.getUserId())) {
                throw new InvalidPlayerException();
            }

            // 3. Retrieve vendor line credentials and secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            //TODO (by Alex), validate gameId is existed in DB and the status is enable
            //TODO (by Alex), should have child game table for save vendor game code by language, platform

            // 4. Validate request signature
            VendorService.validateHash(body, secretKey);

            // 5. Send bet request to Operator and check if player has enough balance
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);

            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(betEvent);

            responseVo.setTransactionId(traceId);
            responseVo.setCurrency(gameSession.getCurrencyCode()); // TODO: vendor currency map
            responseVo.setCash(betEvent.getLastBalance());
            responseVo.setBonus(BigDecimal.ZERO);
            responseVo.setUsedPromo(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            httpRequestLog.setErrorMessage(duplicateExternalTransactionIdException.getMessage());

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setError(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (AuthenticationException authenticationException) {
            responseVo.setError(ResponseCodes.AUTHENTICATION_ERROR);

        } catch (InvalidSignatureException invalidHashException) {
            responseVo.setError(ResponseCodes.INVALID_HASH);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setError(ResponseCodes.INSUFFICIENT_BALANCE);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            httpService.end(httpRequestLog, responseVo);
        }

        //TODO should the trace Id return for all the responses even the request is fail?
        return responseVo;
    }
}
