package com.nextgen.gameaggregator.vendor.inout.api.settle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.inout.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.inout.service.VendorService;
import com.nextgen.gameaggregator.vendor.inout.vo.CommonVo;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;

@Service
public class SettleService {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;
    private final WalletService walletService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public SettleService(VendorService vendorService,
                         HttpService httpService,
                         VendorLineService vendorLineService,
                         WalletService walletService,
                         RequestIdempotentLogService requestIdempotentLogService) {
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo settle(HttpRequestLog httpRequestLog, String xSign){
        CommonVo responseVo = null;
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        String secretKey;
        boolean isRequestExists = false;
        CommonDto<SettleDto> dto = new CommonDto<>();
        try {
            dto = HttpService.convertJsonToDto(body, new TypeReference<>() {
            });

            SettleDto settleDto = dto.getData();

            if (requestIdempotentLogService.checkExists(dto.getData(), dto.getData().getUserId()) == null) {
                requestIdempotentLogService.create(dto.getData(), dto.getData().getUserId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            GameSession gameSession = vendorService.checkGameSession(traceId, settleDto.getUserId(), dto.getGameMode(), dto.getToken());

            VendorLine vendorLine =  vendorLineService.getVendorLineById(gameSession.getVendorLineId());

            secretKey = vendorLineService.getCredentialValueByName(vendorLine.getId(), "SecretKey");

            this.doValidation(dto);

            vendorService.doVerification(dto.getData().getCurrency(), dto.getGameMode(), settleDto.getUserId(),gameSession, secretKey, body, xSign);

            ResultType resultType = settleDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.LOSE;

            BigDecimal betResultAmount = walletService.processBetResult(traceId, gameSession, settleDto, resultType, vendorService, httpRequestLog);

            responseVo.setCode(ResponseCode.OK.code);
            responseVo.setBalance(String.valueOf(betResultAmount));

        } catch (Exception e){
            this.handleException(e, responseVo, httpRequestLog);

        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto.getData(), dto.getData().getUserId());
            }
        }

        return responseVo;

    }

    private void doValidation(CommonDto<SettleDto> dto) throws InvalidRequestException {
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
