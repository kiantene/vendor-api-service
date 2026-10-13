package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.entity.AgentPlayer;
import com.nextgen.gameaggregator.core.entity.Currency;
import com.nextgen.gameaggregator.core.entity.VendorCurrency;
import com.nextgen.gameaggregator.core.entity.VendorGame;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.CurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Covers the operator-token backfill on {@code enrichByGameSession}.
 * <p>
 * The debit path previously did not source {@code token} from the resolved session, unlike
 * {@code CreditContextEnricher} and the bet/result processors. {@code DebitOperatorInvoker} puts
 * {@code context.getToken()} on the {@code OperatorCallTarget}, so for a vendor whose wire payload
 * carries no GA token (JDB, for one) the operator was called with a null session — the asymmetry
 * that made dropping JDB's bogus token mapping unsafe on its own.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DebitContextEnricherTest {

    @Mock private AgentPlayerDataService agentPlayerDataService;
    @Mock private VendorPlayerDataService vendorPlayerDataService;
    @Mock private VendorGameDataService vendorGameDataService;
    @Mock private CurrencyDataService currencyDataService;
    @Mock private VendorCurrencyDataService vendorCurrencyDataService;

    @Mock private VendorPlayer stubVendorPlayer;
    @Mock private AgentPlayer stubAgentPlayer;
    @Mock private VendorGame stubVendorGame;
    @Mock private Currency stubCurrency;
    @Mock private VendorCurrency stubVendorCurrency;

    private DebitContextEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new DebitContextEnricher(agentPlayerDataService, vendorPlayerDataService,
                vendorGameDataService, currencyDataService, vendorCurrencyDataService);

        lenient().when(vendorPlayerDataService.getByUsername(any())).thenReturn(stubVendorPlayer);
        lenient().when(agentPlayerDataService.get(any())).thenReturn(stubAgentPlayer);
        lenient().when(vendorGameDataService.getByVendorGameCodeAndVendorId(any(), any()))
                .thenReturn(stubVendorGame);
        lenient().when(currencyDataService.get(any())).thenReturn(stubCurrency);
        lenient().when(stubCurrency.getCode()).thenReturn("PHP");
        lenient().when(vendorCurrencyDataService.getByVendorIdAndCurrencyId(any(), any()))
                .thenReturn(stubVendorCurrency);
    }

    private DebitContext contextWithoutToken() {
        // traceId is pre-set so doEnrich does not read the (absent) request-scoped LogContext.
        return DebitContext.builder()
                .vendorTxnId("40562652")
                .traceId("trace-1")
                .build();
    }

    private GameSession session() {
        GameSession session = new GameSession();
        session.setToken("dbbc82ce-c01d-4a87-9c1d-442b9169fdfb");
        session.setVendorPlayerUsername("tm63tllpc");
        session.setVendorCurrencyCode("PP");
        session.setVendorGameCode("18_18025");
        session.setCurrencyCode("PHP");
        return session;
    }

    @Test
    void backfillsOperatorTokenFromSession_whenRequestCarriesNone() {
        DebitContext context = contextWithoutToken();

        enricher.enrichByGameSession(context, session());

        assertEquals("dbbc82ce-c01d-4a87-9c1d-442b9169fdfb", context.getToken(),
                "DebitOperatorInvoker sends context.getToken() to the operator; a vendor that "
                        + "returns no GA token must inherit it from the resolved session");
    }

    @Test
    void doesNotOverwriteATokenTheRequestAlreadyCarried() {
        DebitContext context = contextWithoutToken();
        context.setToken("token-from-request");

        enricher.enrichByGameSession(context, session());

        assertEquals("token-from-request", context.getToken(),
                "backfill is a no-op when the vendor genuinely echoed GA's token back");
    }

    @Test
    void stillBackfillsTheOtherSessionFields() {
        DebitContext context = contextWithoutToken();

        enricher.enrichByGameSession(context, session());

        assertEquals("tm63tllpc", context.getVendorPlayerUsername());
        assertEquals("PP", context.getVendorCurrency());
        assertEquals("18_18025", context.getVendorGameCode());
        // currencyCode is then re-sourced from the Currency entity by the BaseEnricher chain;
        // the stub returns the same code the session carried.
        assertEquals("PHP", context.getCurrencyCode());
    }
}
