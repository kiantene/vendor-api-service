package com.nextgen.gameaggregator.vendor.bombay.api.debit;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
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

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class DebitAction {
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
    @Autowired
    private WalletAdjustmentService walletAdjustmentService;
    @Autowired
    private ValidationService validationService;

//    @PostMapping(path = EndPoints.DEBIT)
    @PostMapping(path = "lala")
    public ResponseVo debit(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();

        DebitDto debitDto = null;

        GameSession gameSession = new GameSession();

        try{
            String body = httpRequestLog.getRequestBody();

            debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(debitDto);

            // Verify session token and generate update game session while playing others game
            gameSession = gameSessionService.verifyToken(debitDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(debitDto,gameSession);

            // check db game code is stg or not
            if(gameSession.getVendorGameCode().toLowerCase().contains("_stg")){
                debitDto.setGame_id(debitDto.getGame_id() + "_stg");
            }

            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(debitDto.getGame_id(),gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(debitDto,gameSession);

            // Process Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, httpRequestLog.getRequestBody(), httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
            responseVo.setUser(gameSession.getVendorPlayerUsername());
            responseVo.setBalance(betEvent.getLastBalance().intValue());
            responseVo.setCurrency(gameSession.getCurrencyCode());

            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);

        } catch(AuthenticationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_TOKEN);
        } catch(InsufficientBalanceException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_NOT_ENOUGH_MONEY);
        }
        catch(Exception e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } finally{
            responseVo.setRequest_uuid(debitDto.getRequest_uuid());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(DebitDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(DebitDto dto,GameSession gameSession) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, GameNotSupportedException, CurrencyNotSupportedException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        // Verify vendor gameCode
        String game_code = vendorService.trimGameCode(gameSession.getVendorGameCode());
        ValidationUtils.isEquals(game_code, dto.getGameId(), GameNotSupportedException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }
}
