package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutConfig;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class EvoplayFreeRoundPayoutService
        extends AbstractPromoPayoutController<EvoplayFreeRoundPayoutRequest, EvoplayFreeRoundPayoutResult> {

    public EvoplayFreeRoundPayoutService(EvoplayFreeRoundPayoutRequestMapper requestMapper,
                                         EvoplayFreeRoundPayoutResponseMapper responseMapper,
                                         PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    public EvoplayFreeRoundPayoutResult payout(EvoplayFreeRoundPayoutRequest request, HttpRequestLog httpRequestLog) {
        validateAndLog(request, httpRequestLog);

        try {
            ensureLogContext(httpRequestLog);
            return processRequest(request, context -> context.setHttpRequestLog(httpRequestLog));
        } catch (DuplicateRequestException ex) {
            return fromDuplicate(request, ex);
        } catch (Exception ex) {
            log.error(
                    "Failed to process Evoplay free-round payout, traceId={}, userId={}, eventId={}, transactionId={}, amount={}, currency={}",
                    traceId(httpRequestLog),
                    request.getUserId(),
                    request.getEventId(),
                    request.getId(),
                    request.getAmount(),
                    request.getCurrency(),
                    ex
            );
            return fallback(request);
        }
    }

    private void validateAndLog(EvoplayFreeRoundPayoutRequest request, HttpRequestLog httpRequestLog) {
        try {
            validate(request);
        } catch (IllegalArgumentException ex) {
            log.warn(
                    "Rejected Evoplay free-round payout, traceId={}, userId={}, eventId={}, transactionId={}, amount={}, currency={}, reason={}",
                    traceId(httpRequestLog),
                    userId(request),
                    eventId(request),
                    transactionId(request),
                    amount(request),
                    currency(request),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    private void validate(EvoplayFreeRoundPayoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Evoplay free-round payout request is required");
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException("Evoplay free-round payout amount is required");
        }
        if (request.getAmount().signum() < 0) {
            throw new IllegalArgumentException("Evoplay free-round payout amount must be positive or zero");
        }
    }

    private void ensureLogContext(HttpRequestLog httpRequestLog) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) {
            logContext = new LogContext();
            LogContextHolder.set(logContext);
        }
        if (logContext.getVendorClassName() == null) {
            logContext.setVendorClassName(EndPoints.CLASS_NAME);
        }
        if (httpRequestLog != null && httpRequestLog.getId() != null) {
            logContext.setTraceId(httpRequestLog.getId());
        }
    }

    private EvoplayFreeRoundPayoutResult fromDuplicate(EvoplayFreeRoundPayoutRequest request, DuplicateRequestException ex) {
        return EvoplayFreeRoundPayoutResult.builder()
                .balance(Optional.ofNullable(ex.getBalance()).orElse(BigDecimal.ZERO))
                .currency(Optional.ofNullable(ex.getCurrency()).filter(value -> !value.isBlank()).orElse(request.getCurrency()))
                .build();
    }

    private EvoplayFreeRoundPayoutResult fallback(EvoplayFreeRoundPayoutRequest request) {
        return EvoplayFreeRoundPayoutResult.builder()
                .balance(BigDecimal.ZERO)
                .currency(request.getCurrency())
                .build();
    }

    private String traceId(HttpRequestLog httpRequestLog) {
        return httpRequestLog == null ? null : httpRequestLog.getId();
    }

    private String userId(EvoplayFreeRoundPayoutRequest request) {
        return request == null ? null : request.getUserId();
    }

    private String eventId(EvoplayFreeRoundPayoutRequest request) {
        return request == null ? null : request.getEventId();
    }

    private String transactionId(EvoplayFreeRoundPayoutRequest request) {
        return request == null ? null : request.getId();
    }

    private BigDecimal amount(EvoplayFreeRoundPayoutRequest request) {
        return request == null ? null : request.getAmount();
    }

    private String currency(EvoplayFreeRoundPayoutRequest request) {
        return request == null ? null : request.getCurrency();
    }

    @Override
    protected void configure(PromoPayoutConfig config, EvoplayFreeRoundPayoutRequest request) {
        config.playerUuidCampaignLookup(request.isPlayerUuidCampaignLookup());
    }
}
