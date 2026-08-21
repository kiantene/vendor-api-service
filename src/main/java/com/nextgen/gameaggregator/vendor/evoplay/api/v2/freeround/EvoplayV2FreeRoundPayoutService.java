package com.nextgen.gameaggregator.vendor.evoplay.api.v2.freeround;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayBalanceIncreaseValidator;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutRequest;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutRequestFactory;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutResult;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutService;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Refund;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EvoplayV2FreeRoundPayoutService {
    private final EvoplayBalanceIncreaseValidator validator;
    private final EvoplayFreeRoundPayoutRequestFactory requestFactory;
    private final EvoplayFreeRoundPayoutService payoutService;
    private final EvoplayV2FreeRoundResponseAdapter responseAdapter;
    private final GameSessionService gameSessionService;

    public EvoplayV2FreeRoundPayoutService(EvoplayBalanceIncreaseValidator validator,
                                           EvoplayFreeRoundPayoutRequestFactory requestFactory,
                                           EvoplayFreeRoundPayoutService payoutService,
                                           EvoplayV2FreeRoundResponseAdapter responseAdapter,
                                           GameSessionService gameSessionService) {
        this.validator = validator;
        this.requestFactory = requestFactory;
        this.payoutService = payoutService;
        this.responseAdapter = responseAdapter;
        this.gameSessionService = gameSessionService;
    }

    public boolean supports(CallbackDto callbackDto) {
        return validator.isFreeRoundWin(
                callbackDto == null ? null : callbackDto.getName(),
                callbackDto == null || callbackDto.getData() == null ? null : callbackDto.getData().getType()
        );
    }

    public boolean supportsWin(CallbackDto callbackDto) {
        if (callbackDto == null || callbackDto.getData() == null || !"win".equalsIgnoreCase(callbackDto.getName())) {
            return false;
        }
        var details = callbackDto.getData().getDetailsDto();
        return details != null
                && !isBlank(details.getExtrabonus_registration_id())
                && !isBlank(details.getPayout());
    }

    public ResponseVo payout(CallbackDto callbackDto, HttpRequestLog httpRequestLog) {
        try {
            validator.validateV2(callbackDto);
            EvoplayFreeRoundPayoutRequest payoutRequest = requestFactory.fromV2(callbackDto);
            EvoplayFreeRoundPayoutResult payoutResult = payoutService.payout(payoutRequest, httpRequestLog);
            return responseAdapter.toV2(payoutResult);
        } catch (IllegalArgumentException ex) {
            logInvalidRequest(callbackDto, httpRequestLog, ex);
            return invalidRequest();
        }
    }

    public ResponseVo payoutWin(CallbackDto callbackDto, HttpRequestLog httpRequestLog) {
        try {
            validateWin(callbackDto);
            EvoplayFreeRoundPayoutRequest payoutRequest = requestFactory.fromV2Win(callbackDto);
            EvoplayFreeRoundPayoutResult payoutResult = payoutService.payout(payoutRequest, httpRequestLog);
            return responseAdapter.toV2(payoutResult);
        } catch (IllegalArgumentException ex) {
            logInvalidRequest(callbackDto, httpRequestLog, ex);
            return invalidRequest();
        }
    }

    private ResponseVo invalidRequest() {
        ResponseDataVo error = ResponseDataVo.builder()
                .scope(Scope.INTERNAL)
                .no_refund(Refund.ONE)
                .message(ResponseCodes.INVALID_REQUEST_ERROR.message)
                .build();
        return ResponseVo.builder()
                .status(ResponseCodes.INVALID_REQUEST_ERROR.status)
                .error(error)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateWin(CallbackDto callbackDto) {
        if (callbackDto == null || callbackDto.getData() == null) {
            throw new IllegalArgumentException("data is required");
        }
        String username = callbackDto.getUsername();
        var gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(username);
        if (gameSession == null) {
            throw new IllegalArgumentException("game session is required");
        }
        if (isBlank(callbackDto.getData().getCurrency())) {
            throw new IllegalArgumentException("data.currency is required");
        }
        if (isBlank(gameSession.getVendorCurrencyCode())) {
            throw new IllegalArgumentException("session currency is required");
        }
        ValidationUtils.isEquals(
                gameSession.getVendorCurrencyCode(),
                callbackDto.getData().getCurrency(),
                () -> new IllegalArgumentException("data.currency must match player session currency")
        );
    }

    private void logInvalidRequest(CallbackDto callbackDto, HttpRequestLog httpRequestLog, IllegalArgumentException ex) {
        var data = callbackDto == null ? null : callbackDto.getData();
        log.warn(
                "Rejected Evoplay v2 free-round payout, traceId={}, userId={}, eventId={}, transactionId={}, amount={}, currency={}, reason={}",
                httpRequestLog == null ? null : httpRequestLog.getId(),
                userId(callbackDto),
                data == null ? null : data.getEvent_id(),
                data == null ? null : data.getId(),
                data == null ? null : data.getAmount(),
                data == null ? null : data.getCurrency(),
                ex.getMessage(),
                ex
        );
    }

    private String userId(CallbackDto callbackDto) {
        if (callbackDto == null) {
            return null;
        }
        var data = callbackDto.getData();
        if (data != null && !isBlank(data.getUser_id())) {
            return data.getUser_id();
        }
        return callbackDto.getUsername();
    }
}
