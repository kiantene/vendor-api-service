package com.nextgen.gameaggregator.vendor.bombay.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bombay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bombay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bombay.service.VendorService;
import com.nextgen.gameaggregator.vendor.bombay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class BalanceAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    VendorService vendorService;

    @PostMapping (EndPoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();

        BalanceDto balanceDto = null;

        GameSession gameSession = new GameSession();

        try{
            String body = httpRequestLog.getRequestBody();

            balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);

            String header = vendorService.decodeUnicode(httpRequestLog.getHeader());

            log.info("BB request body: " + body);
            log.info("BB request header: " + header);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(balanceDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
            responseVo.setUser(gameSession.getVendorPlayerUsername());
            responseVo.setBalance(balance.intValue());
            responseVo.setCurrency(gameSession.getCurrencyCode());

        } catch(GameNotSupportedException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_GAME);
        } catch(AuthenticationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_TOKEN);
        } catch(CurrencyNotSupportedException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_WRONG_CURRENCY);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } finally{
            responseVo.setRequest_uuid(balanceDto.getRequest_uuid());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, CurrencyNotSupportedException, GameNotSupportedException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGame_id(), GameNotSupportedException::new);
    }
}
