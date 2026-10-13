package com.nextgen.gameaggregator.core.engine.promo.api;

import com.nextgen.core.api.ApiRequest;
import com.nextgen.gameaggregator.core.engine.promo.campaign.CreatePayoutCampaignRequest;
import com.nextgen.gameaggregator.core.util.OperatorSignatureUtil;
import com.nextgen.gameaggregator.promoengine.PromoEngineProperties;
import com.nextgen.gameaggregator.promoengine.constant.EndPoints;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class PromoEngineApiAdapterTest {

    @Test
    void ofCreatePayoutCampaign_postsTheSignedRequestToTheCreateEndpoint() {
        PromoEngineProperties properties = new PromoEngineProperties();
        properties.setHost("http://promo-engine");
        properties.setApiSecret("secret");
        PromoEngineApiAdapter adapter = new PromoEngineApiAdapter(properties);
        CreatePayoutCampaignRequest body = CreatePayoutCampaignRequest.builder()
                .traceId("trace-1").campaignType(2).vendorId(10).vendorLineId(100)
                .vendorTransactionId("tx-1").currencyCode("USD").vendorCurrencyCode("USD")
                .build();

        ApiRequest request = adapter.ofCreatePayoutCampaign("trace-1", body);

        assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(request.getBaseUrl()).isEqualTo("http://promo-engine");
        assertThat(request.getPath()).isEqualTo(EndPoints.CREATE_PAYOUT_CAMPAIGN);
        assertThat(request.getBody()).isSameAs(body);
        assertThat(request.getHeaders())
                .containsEntry(PromoEngineApiAdapter.HEADER_SIGNATURE, OperatorSignatureUtil.sign(body, "secret"));
    }
}
