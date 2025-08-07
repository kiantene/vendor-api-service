package com.nextgen.gameaggregator.vendor.inout.api.bet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
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
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;
    private final WalletService walletService;

    public BetService(HttpService httpService,
                      GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      VendorService vendorService,
                      WalletService walletService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.vendorService = vendorService;
        this.walletService = walletService;

    }

    public CommonVo bet(HttpRequestLog httpRequestLog, String xSign){
        String traceId = httpRequestLog.getId();
        CommonVo responseVo = new CommonVo();
        String body = httpRequestLog.getRequestBody();
        CommonDto commonDto;
        String secretKey;

        try{
            // 1. Retrieve request body and convert into dto
            CommonDto<BetDto> dto = HttpService.convertJsonToDto(body, new TypeReference<>() {
            });

            BetDto betDto = dto.getData();

            commonDto = HttpService.convertJsonToDto(body, CommonDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betDto.getUserId(), dto.getGameMode());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(commonDto.getGameMode(), gameSession);

            VendorLine vendorLine =  vendorLineService.getVendorLineById(gameSession.getVendorLineId());

            secretKey = vendorLineService.getCredentialValueByName(vendorLine.getId(), "SecretKey");

            // 4. Verify remaining parameters (Verify against database values)
            vendorService.doVerification(dto.getData().getCurrency(), dto.getGameMode(), gameSession, secretKey, body, xSign);

            // 5. Create bet event and process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body, httpRequestLog);

            // 6. Set operator response to responseVO to vendor
            responseVo.setCode(ResponseCode.OK.code);
            responseVo.setBalance(String.valueOf(betEvent.getLastBalance()));

        }catch (Exception e){
            this.handleException(e, responseVo, httpRequestLog);

        }
        return responseVo;

    }

    private void doValidation(CommonDto<BetDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    @ExceptionHandler({InvalidRequestException.class, AuthenticationException.class, Exception.class, InsufficientBalanceException.class})
    private void handleException(Exception e, CommonVo responseVo, HttpRequestLog httpRequestLog) {
        if (e instanceof InvalidRequestException) {
            responseVo.setError(ResponseCode.INVALID_TOKEN);
        } else if (e instanceof AuthenticationException) {
            responseVo.setError(ResponseCode.ACCOUNT_LOCKED);
        }else if (e instanceof InsufficientBalanceException) {
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
