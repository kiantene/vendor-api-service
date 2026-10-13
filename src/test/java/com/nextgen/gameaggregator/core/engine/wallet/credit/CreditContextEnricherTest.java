package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.CurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Covers {@link CreditContextEnricher#applyDebitSnapshot}: a pure step over an enriched context. */
@ExtendWith(MockitoExtension.class)
class CreditContextEnricherTest {

    @Mock private AgentPlayerDataService agentPlayerDataService;
    @Mock private VendorPlayerDataService vendorPlayerDataService;
    @Mock private VendorGameDataService vendorGameDataService;
    @Mock private CurrencyDataService currencyDataService;
    @Mock private VendorCurrencyDataService vendorCurrencyDataService;

    private CreditContextEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new CreditContextEnricher(agentPlayerDataService, vendorPlayerDataService,
                vendorGameDataService, currencyDataService, vendorCurrencyDataService);
    }

    /** As {@code enrichByGameSession} leaves it: agent from today's player lookup. */
    private static CreditContext enriched() {
        CreditContext context = CreditContext.builder()
                .vendorClassName("jdb")
                .vendorTxnId("4288401826")
                .build();
        context.setAgentId(1116);
        context.setAgentPlayerUsername("op-other");
        context.setGameCode("JDB_18_99999");
        context.setCurrencyCode("PHP");
        context.setFromVendorRate(new BigDecimal("1000"));
        return context;
    }

    private static AgentMeta snapshot() {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(1087);
        agentMeta.setUsername("op-tm63ux7pc");
        agentMeta.setCurrency("PHP");
        agentMeta.setGameCode("JDB_18_18029");
        agentMeta.setSession("ga-token-1");
        return agentMeta;
    }

    @Test
    void pinsTheOperatorTargetOnTheDebitsSnapshot() {
        CreditContext context = enriched();

        enricher.applyDebitSnapshot(context, snapshot());

        assertEquals(1087, context.getAgentId(), "the agent the debit took the money from");
        assertEquals("op-tm63ux7pc", context.getAgentPlayerUsername());
        assertEquals("JDB_18_18029", context.getGameCode());
        assertEquals("PHP", context.getCurrencyCode(), "currency is left to the player lookup");
        assertEquals(0, new BigDecimal("1000").compareTo(context.getFromVendorRate()),
                "rates are vendor-currency config, read per request, and left alone");
    }
}
