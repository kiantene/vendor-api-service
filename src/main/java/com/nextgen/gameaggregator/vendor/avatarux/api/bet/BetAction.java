package com.nextgen.gameaggregator.vendor.avatarux.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.avatarux.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.avatarux.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;

    public BetAction(WalletService walletService,
                     HttpService httpService,
                     ValidationService validationService,
                     VendorService vendorService, VendorLineService vendorLineService, GameSessionService gameSessionService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
    }

    @PostMapping(path = EndPoints.TRANSACTION)
    public BetVo betAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        String method = httpRequestLog.getMethod();
        //Add request header log
        httpRequestLog.setRequestBody("Request Body: \n" + httpRequestLog.getRequestBody() + "\nRequest Header: \n" + vendorService.getHeaders(request));
        BetVo betVo = new BetVo();
        BetDto betDto;
        GameSession gameSession;

        try {
            betDto = HttpService.convertJsonToDto(body, BetDto.class);


            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // Verify session
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getNativeId());

            // Verify parameters (Verify against database values)
            this.doVerification(betDto, gameSession, body, method);

            //Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);

            betVo.setBalance(betEvent.getLastBalance());


        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, betVo);
        }
        return betVo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetDto dto, GameSession gameSession, String body, String method) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidRequestException {
        //validate vendor username, agent vendor line, player status, and game status
        if (dto.getType().equals("InitialBet") || dto.getType().equals("PlaceBet")) {
            validationService.validateEligibleBet(gameSession, dto.getNativeId());
        }

    }
}
