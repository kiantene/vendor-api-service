package com.nextgen.gameaggregator.vendor.yesbingo.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@Slf4j
public class BalanceAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    public ResponseVo balance(HttpRequestLog httpRequestLog, String traceId, String decryptedData) {

        ResponseVo responseVo = new ResponseVo();

        try {

            BalanceDto dto = HttpService.convertJsonToDto(decryptedData, BalanceDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUid());

            // Verify request parameters
            this.doVerification(dto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Set Balance and Currency
            responseVo.setBalance(balance);
            responseVo.setStatus(ResponseCodes.SUCCEED);

        } catch (AuthenticationException authenticationException) {
            responseVo.setStatus(ResponseCodes.USER_ID_CANNOT_BE_FOUND);
        } catch (InvalidAgentApiCredentialException | InvalidPlayerException | DisabledAgentPlayerException | DisabledVendorLineException e) {
            responseVo.setStatus(ResponseCodes.NO_AUTHORIZED_ACCESS);
        } catch (InvalidRequestException | JsonProcessingException | DisabledGameException parameterInputErrorException) {
            responseVo.setStatus(ResponseCodes.PARAMETER_INPUT_ERROR);
        } catch (InvalidOperatorResponseException exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);
        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);
        }

        return responseVo;

    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession)
            throws InvalidPlayerException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify if is valid player
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUid(), InvalidPlayerException::new);

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), DisabledGameException::new);
    }
}
