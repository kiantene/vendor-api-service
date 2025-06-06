package com.nextgen.gameaggregator.vendor.aasexy.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aasexy.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aasexy.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@Slf4j
public class BalanceService {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;

    @Autowired
    public BalanceService(HttpService httpService,
                          VendorLineService vendorLineService,
                          AgentPlayerService agentPlayerService,
                          VendorGameService vendorGameService,
                          GameSessionService gameSessionService,
                          WalletService walletService,
                          VendorService vendorService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
    }

    public BalanceVo balance(HttpRequestLog httpRequestLog, String traceId) {

        BalanceVo vo = new BalanceVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // Decode the URL-encoded message
            String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

            // Convert JsonNode back to JSON string
            String convertedJsonString = vendorService.convertBodyToJson(decodedBody);
            RequestDto<BalanceDto> dto = HttpService.convertJsonToDto(convertedJsonString, new TypeReference<>() {
            });

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getMessage().getUserId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            vo.setUserId(dto.getMessage().getUserId());
            vo.setBalance(balance.setScale(3, RoundingMode.DOWN));
            vo.setBalanceTs(vendorService.convertDateTimeFormat(System.currentTimeMillis()));

        } catch (AuthenticationException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_TOKEN);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_USER_ID);
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException | InvalidRequestException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_PARAMETERS);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            vo.setResponseCodes(ResponseCodes.FAIL);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(RequestDto<BalanceDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getMessage());
    }

    private void doVerification(RequestDto<BalanceDto> dto, GameSession gameSession) throws
            InvalidPlayerException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException {

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getMessage().getUserId(), InvalidPlayerException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }

}
