package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
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
    private final PromoPayoutValidator validator;
    private final PromoPayoutProcessor processor;
    private final RequestIdempotencyService requestIdempotencyService;

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        boolean payoutDispatched = false;

        try {
            guard.ensureNotDuplicate(
                    logContext.getVendorClassName(),
                    ACTION,
                    context.getIdempotencyKey()
            );

            enricher.enrich(context);

            validator.validateOrThrow(context);

            PromoPayoutConfig config = state().getConfig();

            // normal flow
            if (!config.isBatch()) {
                PlayerBalanceData result = processor.process(context);
                payoutDispatched = true;
                rememberResponseData(context, result);
                return result;
            }

            processor.processBatch(context).subscribe(); // fire and forget
            payoutDispatched = true;

            PlayerBalanceData result = PlayerBalanceData.getDefault(
                    context.getVendorPlayerUsername(),
                    context.getVendorCurrency()
            );
            rememberResponseData(context, result);
            return result;
        } catch (DuplicateRequestException ex) {
            // The idempotency record belongs to the original request, not this one — leave it alone.
            throw ex;
        } catch (Exception ex) {
            // RequestIdempotentLog is written before any work and has no error state (unlike the wallet
            // flows' GameTransaction), so deleting it is the only way to say "this payout did not happen".
            // Left behind, every retry is answered as a duplicate — for PGSoft an HTTP 200 carrying a zero
            // balance and no error, i.e. success — for a payout the player never received.
            //
            // Dispatch marks the point of no return, and the processor sets it on the context at the
            // operator call itself; the local flag covers a subscribed batch, whose payouts run async.
            // Past either, only bookkeeping remains and freeing the key would risk paying twice.
            if (!payoutDispatched && !context.isPayoutDispatched()) {
                guard.clear();
            }
            throw ex;
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

}
