package com.nextgen.gameaggregator.vendor.jili.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jili.constant.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @PostMapping(path = EndPoints.BET)
    public BetVo BetAction (HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        BetVo betVo = new BetVo();
        String traceId = httpRequestLog.getTraceId();


        try{
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BetDto dto = HttpService.convertJsonToDto(body, BetDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            this.doVerification(dto, gameSession);

            // 3. Retrieve the latest wallet balance from Operator
//            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // 4. Process bet data
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);

            // 5. Process win data
            WinDto winDto = new WinDto();
            winDto.setExternalTransactionId(dto.getReqId());
            winDto.setRoundId(dto.getRoundId());
            winDto.setAmount(dto.getWinloseAmount());
            winDto.setTimestamp(dto.getTimestamp());
            winDto.setWinType(getWinType(dto));
            winDto.setGameId(dto.getGameId());
            winDto.setEffectiveTurnover(dto.getBetAmount());
            BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDto, body);

            // Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));

            betVo.setUsername(gameSession.getVendorPlayerUsername());
            betVo.setCurrency(gameSession.getCurrencyCode());
            betVo.setBalance(betResultEvent.getLastBalance());
            betVo.setToken(gameSession.getToken());


        } catch (InvalidRequestException invalidRequest) {
            betVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
        } catch (AuthenticationException invalidSessionToken) {
            betVo.setResponseCode(ResponseCode.TOKEN_EXPIRED);
        } catch (Exception exception) {
            betVo.setResponseCode(ResponseCode.OTHER_ERROR);
            httpService.logError(httpRequestLog, exception);
        }finally{
            httpService.end(httpRequestLog, betVo);
        }
        return betVo;
    }
    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
    private void doVerification(BetDto dto, GameSession gameSession)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), dto.getToken(), AuthenticationException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 5. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 6. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }

    private WinType getWinType(BetDto dto) {
        return (dto.getWinloseAmount().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
    }
}
