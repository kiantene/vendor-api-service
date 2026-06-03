package com.nextgen.gameaggregator.vendor.spribe.api.result;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.FreebetPayoutContextMapper;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FreebetPayoutContextMapperTest {

    private FreebetPayoutContextMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FreebetPayoutContextMapper();
    }

    private BetResultRequest buildFreebetRequest() {
        BetResultRequest req = new BetResultRequest();
        req.setUserId("bbz6waqpib");
        req.setCurrency("CNY");
        req.setAmount(new BigDecimal("2690"));
        req.setProvider("spribe_aviator");
        req.setProviderTxId("82467397");
        req.setGame("aviator");
        req.setAction("freebet");
        req.setSessionToken("2d9c87e11e7e4b629798d77c40757d30");
        req.setPlatform("desktop");
        req.setActionId("1777436");
        req.setOperatorFreeBetId("019e6c7b95c1779493052fbb22e59c94");
        req.setFreeBetTotalBetAmount(new BigDecimal("5000"));
        return req;
    }

    @Test
    @DisplayName("operatorFreeBetId is mapped to vendorCampaignCode, not actionId")
    void operatorFreeBetId_mappedToVendorCampaignCode() {
        BetResultRequest req = buildFreebetRequest();

        PromoPayoutContext ctx = mapper.toInternal(req);

        assertThat(ctx.getVendorCampaignCode()).isEqualTo("019e6c7b95c1779493052fbb22e59c94");
        assertThat(ctx.getVendorCampaignCode()).isNotEqualTo(req.getActionId());
    }

    @Test
    @DisplayName("vendorCampaignCode is null when operatorFreeBetId is absent")
    void nullOperatorFreeBetId_vendorCampaignCodeIsNull() {
        BetResultRequest req = buildFreebetRequest();
        req.setOperatorFreeBetId(null);

        PromoPayoutContext ctx = mapper.toInternal(req);

        assertThat(ctx.getVendorCampaignCode()).isNull();
    }

    @Test
    @DisplayName("standard fields are mapped correctly")
    void standardFields_mappedCorrectly() {
        BetResultRequest req = buildFreebetRequest();

        PromoPayoutContext ctx = mapper.toInternal(req);

        assertThat(ctx.getIdempotencyKey()).isEqualTo("82467397");
        assertThat(ctx.getVendorTransactionId()).isEqualTo("82467397");
        assertThat(ctx.getVendorPlayerUsername()).isEqualTo("bbz6waqpib");
        assertThat(ctx.getVendorCurrency()).isEqualTo("CNY");
        assertThat(ctx.getVendorGameCode()).isEqualTo("aviator");
        assertThat(ctx.getVendorSessionToken()).isEqualTo("2d9c87e11e7e4b629798d77c40757d30");
        assertThat(ctx.getPromoType()).isEqualTo(PromoType.FREE_ROUND);
        assertThat(ctx.getVendorPayoutAmount())
                .isEqualByComparingTo(AmountConverter.convertUnitToBalance(new BigDecimal("2690")));
    }
}
