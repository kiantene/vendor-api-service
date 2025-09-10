package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromoPayoutServiceImpl implements PromoPayoutService {
    private static final String LOG_GROUP = "promo";
    private static final String ACTION = "payout";
    private final DuplicateRequestGuard guard;
    private final BaseEnricher<PromoPayoutContext> enricher;
    private final CoreEngineProcessor<PromoPayoutContext, ClientBalanceResponse> processor;

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);

        try {
            guard.ensureNotDuplicate(logContext.getVendorClassName(), ACTION, context.getIdempotencyKey());

            enricher.enrich(context);

            return processor.process(context);
        } catch (EntityNotFoundException ex) {
            throw new InternalConfigurationException(ex.getMessage(), ex);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
        }
    }
}
