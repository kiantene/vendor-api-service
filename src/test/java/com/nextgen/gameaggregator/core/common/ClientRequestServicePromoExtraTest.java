package com.nextgen.gameaggregator.core.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.config.properties.WalletServiceProperties;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutDto;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutExtra;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The promo payout {@code extra} block reaches ONLY the internal transfer wallet.
 *
 * <p>{@code PromoPayoutMapper} fills it for every agent because it cannot tell them apart -
 * {@code PromoPayoutContext} has no seamless type - so the gate is
 * {@link ClientRequestService#stripPromoExtraForNonTransferWallet}, run while the outgoing request is
 * built. Without it, adding the field for the transfer wallet silently changes the payload every
 * seamless operator already parses.
 *
 * <p>Mirrors {@code ClientRequestServiceRollbackMetaTest}, which covers the same arrangement for
 * {@code RollbackMeta}.
 */
class ClientRequestServicePromoExtraTest {

    private static final String TW_HOST = "http://transfer-wallet";
    private static final String OPERATOR_CALLBACK = "http://some-operator/api";
    private static final Integer AGENT_ID = 100;

    // --- the helper in isolation ----------------------------------------------------

    @Test
    void transferWallet_keepsExtra() {
        PromoPayoutDto dto = promoPayoutWithExtra();

        newService().stripPromoExtraForNonTransferWallet(dto, true); // seamless-transfer agent

        assertNotNull(dto.getExtra(), "extra must be kept for the transfer wallet");
    }

    @Test
    void nonTransferWallet_stripsExtra() {
        PromoPayoutDto dto = promoPayoutWithExtra();

        newService().stripPromoExtraForNonTransferWallet(dto, false); // any other operator

        assertNull(dto.getExtra(), "extra must NOT be sent to normal operators");
    }

    @Test
    void nonPromoPayoutRequest_isNoOp() {
        // A non-PromoPayoutDto request must be ignored without error.
        newService().stripPromoExtraForNonTransferWallet(new Object(), false);
    }

    @Test
    void transferWallet_nullExtra_staysNull() {
        PromoPayoutDto dto = promoPayoutDto().build(); // no extra set

        newService().stripPromoExtraForNonTransferWallet(dto, true);

        assertNull(dto.getExtra());
    }

    // --- end-to-end: the strip is actually wired into createOperatorApiRequest -------

    @Test
    void createOperatorApiRequest_seamlessAgent_extraNeverSent() {
        OperatorApiRequest req = promoPayoutRequestFor(SeamlessType.SEAMLESS);

        assertNull(((PromoPayoutDto) req.getBody()).getExtra(),
                "extra must NEVER reach an operator that did not ask for it");
    }

    @Test
    void createOperatorApiRequest_seamlessTransferAgent_extraSent() {
        OperatorApiRequest req = promoPayoutRequestFor(SeamlessType.SEAMLESS_TRANSFER);

        PromoPayoutExtra extra = ((PromoPayoutDto) req.getBody()).getExtra();
        assertNotNull(extra, "extra must be sent to the transfer wallet");
        assertEquals("1234567", extra.getPlayerId());
    }

    /** The stripped field must be gone from the wire payload, not serialized as {@code "extra": null}. */
    @Test
    void strippedExtraIsAbsentFromTheJsonSentToAnOperator() throws Exception {
        OperatorApiRequest req = promoPayoutRequestFor(SeamlessType.SEAMLESS);

        String json = new ObjectMapper().writeValueAsString(req.getBody());

        assertTrue(!json.contains("extra"), () -> "extra leaked into the payload: " + json);
    }

    /**
     * The signature must cover what is actually sent. The strip runs before {@code getHeaders}, so a
     * seamless agent's signature differs from the transfer wallet's for the same logical payout - if
     * the order ever flipped, operators would reject every promo payout as a signature mismatch.
     */
    @Test
    void signatureCoversTheStrippedPayload() {
        String operatorSignature = promoPayoutRequestFor(SeamlessType.SEAMLESS)
                .getHeaders().get(ClientRequestService.HEADER_SIGNATURE);

        PromoPayoutDto alreadyStripped = promoPayoutDto().build();
        String expected = com.nextgen.gameaggregator.core.util.OperatorSignatureUtil
                .sign(alreadyStripped, "agent-api-secret");

        assertEquals(expected, operatorSignature, "signature must be taken over the payload sent");
    }

    // --- fixtures -------------------------------------------------------------------

    private ClientRequestService newService() {
        WalletServiceProperties props = new WalletServiceProperties();
        props.setHost(TW_HOST);
        // The gate is a boolean; the other deps are unused by the helper.
        return new ClientRequestService(null, null, null, props);
    }

    private static PromoPayoutDto.PromoPayoutDtoBuilder promoPayoutDto() {
        return PromoPayoutDto.builder()
                .traceId("trace-promo-001")
                .username("player001")
                .transactionId("0199c1f2a7b47d3e9f51c8a2b6d04e77")
                .campaignId("cmp-summer-88")
                .promoType("TOURNAMENT")
                .currency("USD")
                .amount(new BigDecimal("25.50"))
                .timestamp(1789430400000L);
    }

    private static PromoPayoutDto promoPayoutWithExtra() {
        PromoPayoutExtra extra = new PromoPayoutExtra();
        extra.setPlayerId("1234567");
        return promoPayoutDto().extra(extra).build();
    }

    private OperatorApiRequest promoPayoutRequestFor(SeamlessType seamlessType) {
        Validator validator = mock(Validator.class);
        when(validator.validate(any())).thenAnswer(i -> Collections.emptySet());

        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setSeamlessType(seamlessType.code);

        AgentApiCredential credential = new AgentApiCredential();
        credential.setApiKey("agent-api-key");
        credential.setApiSecret("agent-api-secret");
        credential.setCallbackUrl(OPERATOR_CALLBACK);
        credential.setAgent(agent);

        AgentApiCredentialDataService credentialService = mock(AgentApiCredentialDataService.class);
        when(credentialService.getActiveCredential(AGENT_ID)).thenReturn(credential);

        WalletServiceProperties props = new WalletServiceProperties();
        props.setHost(TW_HOST);

        ClientRequestService service = new ClientRequestService(
                validator, credentialService, mock(AgentDataService.class), props);

        return service.createOperatorApiRequest(
                "trace-promo-001", AGENT_ID, "player001",
                EndPoints.PROMO_PAYOUT, promoPayoutWithExtra(), 1789430400000L);
    }
}
