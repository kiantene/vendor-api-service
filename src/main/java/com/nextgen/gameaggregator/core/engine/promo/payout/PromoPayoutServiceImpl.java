package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class PromoPayoutServiceImpl implements PromoPayoutService {
    private static final String LOG_GROUP = "promo";
    private static final String ACTION = "payout";
    private final DuplicateRequestGuard guard;
    private final PromoPayoutContextEnricher enricher;
    private final PromoPayoutProcessor processor;

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);

        try {
            guard.ensureNotDuplicate(logContext.getVendorClassName(), ACTION, context.getIdempotencyKey());

            enricher.enrich(context);

            PromoPayoutConfig config = state().getConfig();

            if (!config.isBatch()) {
                return processor.process(context);
            }

            processor.processBatch(context).subscribe(); // fire and forget

            return PlayerBalanceData.getDefault(
                    context.getVendorPlayerUsername(),
                    context.getVendorCurrency()
            );
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
        }
    }

    @Override
    public PromoPayoutService initialise(PromoPayoutContext context) {
        PromoPayoutWrapperContext state = new PromoPayoutWrapperContext(context);
        PromoPayoutContextHolder.set(state);
        return this;
    }

    @Override
    public PromoPayoutService configure(Consumer<PromoPayoutConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }

    private PromoPayoutWrapperContext state() {
        return PromoPayoutContextHolder.getRequired();
    }

    private void cleanup() {
        guard.cleanup();
        PromoPayoutContextHolder.clear();
    }
}
