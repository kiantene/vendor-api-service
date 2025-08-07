package com.nextgen.gameaggregator.vendor.inout.api.bet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.constant.Credentials;
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
    private final RequestIdempotentLogService requestIdempotentLogService;

    public BetService(HttpService httpService,
                      GameSessionService gameSessionService,
                      VendorLineService vendorLineService,
                      VendorService vendorService,
                      WalletService walletService,
                      RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.vendorService = vendorService;
        this.walletService = walletService;

        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo bet(HttpRequestLog httpRequestLog, String xSign){
        String traceId = httpRequestLog.getId();
        CommonVo responseVo = new CommonVo();
        String body = httpRequestLog.getRequestBody();
        String secretKey;
        boolean isRequestExists = false;
        CommonDto<BetDto> dto = new CommonDto<>();

        try{
            // 1. Retrieve request body and convert into dto
            dto = HttpService.convertJsonToDto(body, new TypeReference<>() {
            });

            BetDto betDto = dto.getData();

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            if (requestIdempotentLogService.checkExists(dto.getData(), dto.getData().getUserId()) == null) {
                requestIdempotentLogService.create(dto.getData(), dto.getData().getUserId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betDto.getUserId(), dto.getGameMode());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGameMode(), gameSession);

            secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Verify remaining parameters (Verify against database values)
            vendorService.doVerification(dto.getData().getCurrency(), dto.getGameMode(), betDto.getUserId(), gameSession, secretKey, body, xSign);

            // 5. Create bet event and process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body, httpRequestLog);

            // 6. Set operator response to responseVO to vendor
            responseVo.setCode(ResponseCode.OK.code);
            responseVo.setBalance(String.valueOf(betEvent.getLastBalance()));

        }catch (Exception e){
            this.handleException(e, responseVo, httpRequestLog);

        }finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto.getData(), dto.getData().getUserId());
            }
        }
        return responseVo;

    }

    private void doValidation(CommonDto<BetDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    @ExceptionHandler({InvalidRequestException.class, AuthenticationException.class, Exception.class, InsufficientBalanceException.class})
    private void handleException(Exception e, CommonVo responseVo, HttpRequestLog httpRequestLog) {
        vendorService.exceptionHandler(e, responseVo);
        httpService.logError(httpRequestLog, e);
    }
}
