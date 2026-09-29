package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.config.properties.WalletServiceProperties;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutDto;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.AgentApiCredential;
import com.nextgen.gameaggregator.core.service.AgentApiCredentialDataService;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.enums.SeamlessType;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Where a promo payout is addressed, per agent type.
 *
 * <p>A seamless-transfer (transfer wallet) agent's promo payout must leave GA pointed at the internal
 * wallet service rather than the operator's own callback URL - but nothing in the promo flow asks for
 * that. {@code PromoPayoutProcessor}, {@code PromoPayoutContextEnricher} and
 * {@code PromoPayoutValidator} never look at {@code seamlessType}; the routing is inherited from the
 * override inside {@link ClientRequestService#createOperatorApiRequest}, which every outbound operator
 * call funnels through. Promo payout therefore got transfer-wallet support implicitly, and would lose
 * it just as implicitly if that override moved or the endpoint constant changed.
 *
 * <p>Nothing else would notice. {@code PromoPayoutProcessor} catches every failure from the operator
 * call, enqueues a retry and returns a zero-balance default, reporting success to the vendor - so a
 * misrouted payout looks exactly like a successful one (GA-14899).
 *
 * <p>Uses real {@code AgentApiCredential}/{@code Agent} entities rather than mocks on purpose: the
 * override works by calling {@code setCallbackUrl} on the loaded credential, and a mock would swallow
 * that mutation and let the assertions pass on the stubbed value instead.
 */
class ClientRequestServicePromoPayoutRoutingTest {

    private static final String TW_HOST = "http://transfer-wallet";
    private static final String TW_CALLBACK = TW_HOST + "/seamless"; // == WalletServiceProperties.getCallbackUrl()
    private static final String OPERATOR_CALLBACK = "http://some-operator/api";
    private static final Integer AGENT_ID = 100;
    private static final String API_KEY = "agent-api-key";

    /**
     * The path the internal wallet service publishes for this endpoint. Kept as a literal so a rename
     * of {@link EndPoints#PROMO_PAYOUT} fails here instead of silently 404ing in production: the
     * receiver lives in a separate repo (fortress-wallet-ms, {@code PromoPayoutAction}) and cannot be
     * refactored along with this one.
     */
    private static final String WALLET_SERVICE_PROMO_PAYOUT_PATH = "/v1/promo/payout";

    @Test
    void seamlessTransferAgent_promoPayoutAddressedToWalletService() {
        // The agent's stored callback URL is the operator's own - it must be overridden, not used.
        OperatorApiRequest req = promoPayoutRequestFor(SeamlessType.SEAMLESS_TRANSFER);

        assertEquals(TW_CALLBACK, req.getBaseUrl(), "transfer-wallet agent must not reach the operator");
        assertEquals(EndPoints.PROMO_PAYOUT, req.getPath());
        assertEquals(TW_CALLBACK + WALLET_SERVICE_PROMO_PAYOUT_PATH, req.getUrl(),
                "must compose the wallet service's promo payout endpoint");
    }

    @Test
    void seamlessAgent_promoPayoutAddressedToOperatorCallback() {
        OperatorApiRequest req = promoPayoutRequestFor(SeamlessType.SEAMLESS);

        assertEquals(OPERATOR_CALLBACK, req.getBaseUrl(), "a normal operator's callback must be left alone");
        assertEquals(OPERATOR_CALLBACK + WALLET_SERVICE_PROMO_PAYOUT_PATH, req.getUrl());
    }

    /**
     * The wallet service authenticates promo payout with the <b>agent's</b> API key, the same as
     * {@code /wallet/bet}. This differs from the operator-initiated transfer leg ({@code cash/deposit}
     * and friends), which uses GA's own wallet-service access key - don't unify the two.
     */
    @Test
    void seamlessTransferAgent_signedWithAgentCredential() {
        OperatorApiRequest req = promoPayoutRequestFor(SeamlessType.SEAMLESS_TRANSFER);

        assertEquals(API_KEY, req.getHeaders().get(ClientRequestService.HEADER_API_KEY));
    }

    /** Guards the path GA and the wallet service agreed on; see the constant's javadoc. */
    @Test
    void promoPayoutPath_matchesWalletServiceContract() {
        assertEquals(WALLET_SERVICE_PROMO_PAYOUT_PATH, EndPoints.PROMO_PAYOUT);
    }

    private OperatorApiRequest promoPayoutRequestFor(SeamlessType seamlessType) {
        Validator validator = mock(Validator.class);
        when(validator.validate(any())).thenAnswer(i -> Collections.emptySet());

        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setSeamlessType(seamlessType.code);

        AgentApiCredential credential = new AgentApiCredential();
        credential.setApiKey(API_KEY);
        credential.setApiSecret("agent-api-secret");
        credential.setCallbackUrl(OPERATOR_CALLBACK);
        credential.setAgent(agent); // eager @ManyToOne in production; avoids the agent-cache fallback

        AgentApiCredentialDataService credentialService = mock(AgentApiCredentialDataService.class);
        when(credentialService.getActiveCredential(AGENT_ID)).thenReturn(credential);

        WalletServiceProperties props = new WalletServiceProperties();
        props.setHost(TW_HOST);

        ClientRequestService service = new ClientRequestService(
                validator, credentialService, mock(AgentDataService.class), props);

        return service.createOperatorApiRequest(
                "trace-promo-001",
                AGENT_ID,
                "player001",
                EndPoints.PROMO_PAYOUT,
                promoPayoutDto(),
                1789430400000L
        );
    }

    private static PromoPayoutDto promoPayoutDto() {
        return PromoPayoutDto.builder()
                .traceId("trace-promo-001")
                .username("player001")
                .transactionId("0199c1f2a7b47d3e9f51c8a2b6d04e77")
                .campaignId("cmp-summer-88")
                .promoType("TOURNAMENT")
                .currency("USD")
                .amount(new BigDecimal("25.50"))
                .timestamp(1789430400000L)
                .build();
    }
}
