package com.nextgen.gameaggregator.vendor.inout.api.bet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.inout.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.inout.service.VendorService;
import com.nextgen.gameaggregator.vendor.inout.vo.CommonVo;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Service
public class BetService {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final WalletService walletService;

    public BetService(HttpService httpService,
                      GameSessionService gameSessionService,
                      ValidationService validationService,
                      VendorService vendorService,
                      WalletService walletService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.walletService = walletService;
    }

    public CommonVo bet(HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();
        CommonVo responseVo = new CommonVo();
        String body = httpRequestLog.getRequestBody();
        CommonDto commonDto;

        try {
            // 1. Retrieve request body and convert into dto
            CommonDto<BetDto> dto = HttpService.convertJsonToDto(body, new TypeReference<>() {
            });

            BetDto betDto = dto.getData();

            commonDto = HttpService.convertJsonToDto(body, CommonDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(commonDto.getGameMode(), gameSession);

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // 5. Create bet event and process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body, httpRequestLog);

            // 6. Set operator response to responseVO to vendor
            responseVo.setCode(ResponseCode.OK.code);
            responseVo.setBalance(String.valueOf(betEvent.getLastBalance()));

        } catch (Exception e) {
            this.handleException(e, responseVo, httpRequestLog);

        }
        return responseVo;

    }

    private void doValidation(CommonDto<BetDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CommonDto<BetDto> dto, GameSession gameSession) throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException, InvalidPlayerException {
        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getData().getUserId());

        // 2. Verify Currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getData().getCurrency(), AuthenticationException::new);

        // 3. Verify GameMode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameMode(), AuthenticationException::new);
    }


    @ExceptionHandler({InvalidRequestException.class, AuthenticationException.class, Exception.class, InsufficientBalanceException.class})
    private void handleException(Exception e, CommonVo responseVo, HttpRequestLog httpRequestLog) {
        if (e instanceof InvalidRequestException) {
            responseVo.setError(ResponseCode.INVALID_TOKEN);
        } else if (e instanceof AuthenticationException) {
            responseVo.setError(ResponseCode.ACCOUNT_LOCKED);
        } else if (e instanceof InsufficientBalanceException) {
            responseVo.setError(ResponseCode.INSUFFICIENT_FUNDS);
        } else if (e instanceof DisabledVendorLineException ||
                e instanceof DisabledGameException ||
                e instanceof DisabledAgentPlayerException) {
            responseVo.setError(ResponseCode.GAME_DISABLED);
        } else {
            responseVo.setError(ResponseCode.UNKNOWN_ERROR);
        }

        httpService.logError(httpRequestLog, e);
    }
}
