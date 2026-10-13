package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FetchCampaignResponseMapperTest {

    private final FetchCampaignResponseMapper mapper = new FetchCampaignResponseMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsASuccessfulResponse() throws Exception {
        Campaign campaign = mapper.responseToCampaign(parse("""
                {"traceId":"trace-1","status":"SC_OK","data":{"uuid":"c-uuid","status":2,"campaignName":"Free Spins"}}
                """));

        assertThat(campaign.getUuid()).isEqualTo("c-uuid");
        assertThat(campaign.getCampaignName()).isEqualTo("Free Spins");
    }

    @Test
    void returnsAnEmptyCampaignOnANonOkStatus() throws Exception {
        Campaign campaign = mapper.responseToCampaign(parse("""
                {"traceId":"trace-1","status":"SC_INVALID_CAMPAIGN_RESPONSE"}
                """));

        assertThat(campaign.getUuid()).isNull();
    }

    /** Comparing from the constant side keeps a missing status from throwing a NullPointerException. */
    @Test
    void returnsAnEmptyCampaignOnAMissingStatus() throws Exception {
        Campaign campaign = mapper.responseToCampaign(parse("""
                {"traceId":"trace-1"}
                """));

        assertThat(campaign.getUuid()).isNull();
    }

    private FetchCampaignResponse parse(String json) throws Exception {
        return objectMapper.readValue(json, FetchCampaignResponse.class);
    }
}
