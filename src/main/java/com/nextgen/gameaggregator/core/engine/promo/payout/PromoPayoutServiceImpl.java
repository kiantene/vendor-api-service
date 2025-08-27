package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.common.ContextValidator;
import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutServiceImpl implements PromoPayoutService {

    private final ContextValidator<PromoPayoutContext> validator;
    private final BaseEnricher<PromoPayoutContext> enricher;
    private final CoreEngineProcessor<PromoPayoutContext, ClientBalanceResponse> processor;

    public PromoPayoutServiceImpl(PromoPayoutValidator validator,
                                  PromoPayoutContextEnricher enricher,
                                  PromoPayoutProcessor processor) {

        this.validator = validator;
        this.enricher = enricher;
        this.processor = processor;
    }

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("PromoPayout");

        try {
            validator.validateOrThrow(context);
            enricher.enrich(context);
            return processor.process(context);
        } finally {
            LogContextService.updateLogContextFromHttpRequestLog(logContext, context.getHttpRequestLog());
        }
    }
}
