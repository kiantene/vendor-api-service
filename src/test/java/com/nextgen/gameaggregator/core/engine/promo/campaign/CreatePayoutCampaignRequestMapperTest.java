package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePayoutCampaignRequestMapperTest {

    private final CreatePayoutCampaignRequestMapper mapper = new CreatePayoutCampaignRequestMapper();

    @Test
    void mapsEveryFieldOntoTheRequest() {
        CreatePayoutCampaignRequest request = mapper.toCreatePayoutCampaignRequest(
                "trace-1", 2, 10, 100, "12345", "SUMMER_SPRINT", "tx-1", "USD", "USD", 1_700_000_000_000L);

        assertThat(request.getTraceId()).isEqualTo("trace-1");
        assertThat(request.getCampaignType()).isEqualTo(2);
        assertThat(request.getVendorId()).isEqualTo(10);
        assertThat(request.getVendorLineId()).isEqualTo(100);
        assertThat(request.getVendorCampaignCode()).isEqualTo("12345");
        assertThat(request.getVendorCampaignName()).isEqualTo("SUMMER_SPRINT");
        assertThat(request.getVendorTransactionId()).isEqualTo("tx-1");
        assertThat(request.getCurrencyCode()).isEqualTo("USD");
        assertThat(request.getVendorCurrencyCode()).isEqualTo("USD");
        assertThat(request.getPayoutTime()).isEqualTo(1_700_000_000_000L);
    }

    /**
     * The promo engine's PayoutCampaignDto requires campaignType; a vendor-api name like promoType would
     * fail its validation on every call. A plain ObjectMapper is what the HMAC signer uses, so the signed
     * payload carries the same names.
     */
    @Test
    void serializesCampaignTypeUnderThePromoEngineName() throws Exception {
        CreatePayoutCampaignRequest request = mapper.toCreatePayoutCampaignRequest(
                "trace-1", 6, 10, 100, "9001", null, "tx-1", "EUR", "EUR", null);

        JsonNode json = new ObjectMapper().valueToTree(request);

        assertThat(json.get("campaignType").asInt()).isEqualTo(6);
        assertThat(json.has("promoType")).isFalse();
    }

    /** No code or name lets the promo engine derive a per-payout code and a placeholder name. */
    @Test
    void leavesNullFieldsOutOfThePayload() throws Exception {
        CreatePayoutCampaignRequest request = mapper.toCreatePayoutCampaignRequest(
                "trace-1", 6, 10, 100, null, null, "tx-1", "EUR", "EUR", null);

        JsonNode json = new ObjectMapper().valueToTree(request);

        assertThat(json.has("vendorCampaignCode")).isFalse();
        assertThat(json.has("vendorCampaignName")).isFalse();
        assertThat(json.has("payoutTime")).isFalse();
        assertThat(json.get("vendorTransactionId").asText()).isEqualTo("tx-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void treatsABlankCodeAndNameAsAbsent(String blank) throws Exception {
        CreatePayoutCampaignRequest request = mapper.toCreatePayoutCampaignRequest(
                "trace-1", 6, 10, 100, blank, blank, "tx-1", "EUR", "EUR", null);

        assertThat(request.getVendorCampaignCode()).isNull();
        assertThat(request.getVendorCampaignName()).isNull();
        JsonNode json = new ObjectMapper().valueToTree(request);
        assertThat(json.has("vendorCampaignCode")).isFalse();
        assertThat(json.has("vendorCampaignName")).isFalse();
    }
}
