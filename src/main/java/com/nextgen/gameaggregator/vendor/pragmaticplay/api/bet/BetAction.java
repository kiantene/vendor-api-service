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
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    @Autowired
    private AgentApiCredentialService agentApiCredentialService;

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
            this.doValidate(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Verify received username differs from game session
            ValidationUtils.validateEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

            // 4. Retrieve vendor line credentials and secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            // 5. Validate request signature
            VendorService.validateHash(body, secretKey);

            // 6. Validate game login token gameId whether match with bet request
            // TODO: review this exception
            ValidationUtils.validateEquals(gameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);

            // 7. Verify vendor line status
            vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

            // 8. Verify Agent Player status
            agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

            // 9. Verify Agent API Credential status
            agentApiCredentialService.verifyAgentStatus(gameSession.getAgentId());

            // 10. Verify Vendor Game status
            vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

            //TODO (by Alex), should have child game table for save vendor game code by language, platform

            // 11. Send bet request to Operator and check if player has enough balance
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

        } catch (DisableAgentPlayerException disableAgentPlayerException) {
            responseVo.setError(ResponseCodes.PLAYER_FROZEN);

        } catch (DisableAgentException disableAgentException) {
            // TODO: to review response code
            responseVo.setError(ResponseCodes.PLAYER_FROZEN);

        } catch (AuthenticationException authenticationException) {
            responseVo.setError(ResponseCodes.AUTHENTICATION_ERROR);

        } catch (InvalidSignatureException invalidHashException) {
            responseVo.setError(ResponseCodes.INVALID_HASH);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setError(ResponseCodes.INSUFFICIENT_BALANCE);

        } catch (DisableVendorLineException disableVendorLineException) {
            responseVo.setError(ResponseCodes.BET_NOT_ALLOWED);

        } catch (DisableGameException disableGameException) {
            responseVo.setError(ResponseCodes.INVALID_GAME);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }

    private void doValidate(BetDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        ValidationUtils.validateEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }
}
