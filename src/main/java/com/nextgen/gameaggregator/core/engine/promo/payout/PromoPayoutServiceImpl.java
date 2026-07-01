package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceProcessor;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.idempotency.RequestIdempotencyService;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class PromoPayoutServiceImpl implements PromoPayoutService {
    private static final String LOG_GROUP = "promo";
    private static final String ACTION = "payout";
    private final DuplicateRequestGuard guard;
    private final PromoPayoutContextEnricher enricher;
    private final PromoPayoutProcessor processor;
    private final BalanceProcessor balanceProcessor;
    private final RequestIdempotencyService requestIdempotencyService;

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);

        try {
            guard.ensureNotDuplicate(
                    logContext.getVendorClassName(),
                    ACTION,
                    context.getIdempotencyKey()
            );

            enricher.enrich(context);

            if (!state().getConfig().isCallOperatorOnZeroPayout() && !hasPayout(context)) {
                // if no payout amount, return player balance to vendor
                // if get balance failed, return default zero balance
                PlayerBalanceData result = balanceProcessor.process(logContext.getTraceId(), context);
                rememberResponseData(context, result);
                return result;
            }

            PromoPayoutConfig config = state().getConfig();

            // normal flow
            if (!config.isBatch()) {
                PlayerBalanceData result = processor.process(context);
                rememberResponseData(context, result);
                return result;
            }

            processor.processBatch(context).subscribe(); // fire and forget

            PlayerBalanceData result = PlayerBalanceData.getDefault(
                    context.getVendorPlayerUsername(),
                    context.getVendorCurrency()
            );
            rememberResponseData(context, result);
            return result;
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

    private void rememberResponseData(PromoPayoutContext context, PlayerBalanceData result) {
        BigDecimal balance = result.getBalance() != null ? result.getBalance() : BigDecimal.ZERO;
        requestIdempotencyService.enrichIdempotentLog(
                context.getTransactionId(),
                context.getVendorCurrency(),
                balance
        );
    }

    private boolean hasPayout(PromoPayoutContext context) {
        if (context.getPayoutTransactions() == null || context.getPayoutTransactions().isEmpty()) {
            return context.getVendorPayoutAmount() != null && context.getVendorPayoutAmount().signum() > 0;
        }

        return context.getPayoutTransactions()
                .stream()
                .anyMatch(transaction ->
                        transaction.getVendorPayoutAmount() != null && transaction.getVendorPayoutAmount().signum() > 0);

    }
}
