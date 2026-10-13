package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreatePayoutCampaignResponseMapperTest {

    private final CreatePayoutCampaignResponseMapper mapper = new CreatePayoutCampaignResponseMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** The promo engine serializes its ResponseCodes enum by name, so success arrives as "SC_OK". */
    @Test
    void mapsASuccessfulResponse() throws Exception {
        Campaign campaign = mapper.responseToCampaign(parse("""
                {"traceId":"trace-1","status":"SC_OK","message":"success",
                 "data":{"uuid":"c-uuid","status":3,"campaignName":"TOURNAMENT 12345","completedAt":1700000000000}}
                """));

        assertThat(campaign.getUuid()).isEqualTo("c-uuid");
        assertThat(campaign.getStatus()).isEqualTo(3);
        assertThat(campaign.getCampaignName()).isEqualTo("TOURNAMENT 12345");
    }

    /**
     * An empty campaign would be cached by vendor campaign code and leave every payout of that campaign
     * unlabelled until the entry expired, so a failure has to surface.
     */
    @Test
    void throwsOnANonOkStatus() throws Exception {
        CreatePayoutCampaignResponse response = parse("""
                {"traceId":"trace-1","status":"SC_INVALID_REQUEST","message":"Request validation failed"}
                """);

        assertThatThrownBy(() -> mapper.responseToCampaign(response))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SC_INVALID_REQUEST")
                .hasMessageContaining("Request validation failed");
    }

    @Test
    void throwsOnAMissingStatus() throws Exception {
        CreatePayoutCampaignResponse response = parse("""
                {"traceId":"trace-1","data":{"uuid":"c-uuid"}}
                """);

        assertThatThrownBy(() -> mapper.responseToCampaign(response))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void throwsOnAnOkStatusWithNoData() throws Exception {
        CreatePayoutCampaignResponse response = parse("""
                {"traceId":"trace-1","status":"SC_OK","message":"success"}
                """);

        assertThatThrownBy(() -> mapper.responseToCampaign(response))
                .isInstanceOf(IllegalStateException.class);
    }

    private CreatePayoutCampaignResponse parse(String json) throws Exception {
        return objectMapper.readValue(json, CreatePayoutCampaignResponse.class);
    }
}
