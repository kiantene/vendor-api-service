package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromoWinRequestMapperTest {

    private final PromoWinRequestMapper mapper = new PromoWinRequestMapper();

    @ParameterizedTest(name = "opt {0} ({1}) -> {2}")
    @CsvSource({
            "26, Jackpot Win,        JACKPOT",
            "85, TournamentPrizeWin, TOURNAMENT"
    })
    void mapsEachOperationTypeToItsPromoType(String opt, String vendorName, PromoType expected) {
        assertThat(mapper.toInternal(PromoWinRequests.of(opt)).getPromoType()).isEqualTo(expected);
    }

    /**
     * "These wins do not have a bet associated with them" is the endpoint's own definition, so a paired
     * win type arriving here would mean settled bets are being routed to the promo payout. {@code 5}
     * In-game Gift Win is the win half of {@code 4} In-game Gift Bet, {@code 35} Tournament Win of
     * {@code 34} Tournament Bet — both settle through /bet and /result.
     */
    @ParameterizedTest(name = "opt {0} has a bet behind it and does not belong here")
    @ValueSource(strings = {"5", "35"})
    void rejectsPairedWinTypesThatBelongOnTheBetFlow(String opt) {
        assertThatThrownBy(() -> mapper.toInternal(PromoWinRequests.of(opt)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(opt);
    }

    @Test
    void mapsTheVendorFieldsOntoTheContext() {
        PromoPayoutContext context = mapper.toInternal(PromoWinRequests.of("26"));

        assertThat(context.getIdempotencyKey()).isEqualTo("40439c1a12cbe8c68df1");
        assertThat(context.getVendorTransactionId()).isEqualTo("40439c1a12cbe8c68df1");
        assertThat(context.getVendorPlayerUsername()).isEqualTo("65441");
        // cid is the player's currency, not a brand id — the bet-flow mapper set no currency at all.
        assertThat(context.getVendorCurrency()).isEqualTo("EUR");
        assertThat(context.getVendorPayoutAmount()).isEqualByComparingTo("75.3");
    }

    /**
     * {@code pmid} identifies the Digitain promotion, so every payout from it lands on one campaign created
     * by the promo engine. No resolve key is set: there is no Digitain campaign of ours to look up.
     */
    @Test
    void keysTheCampaignOnThePromotionId() {
        PromoPayoutContext context = mapper.toInternal(PromoWinRequests.of("26"));

        assertThat(context.getVendorCampaignCode()).isEqualTo("9001");
        assertThat(context.getVendorFreeRoundBonusId()).isNull();
    }

    /** No {@code pmid} still pays; the promo engine derives one campaign per payout from {@code txid}. */
    @Test
    void leavesTheCampaignCodeUnsetWithoutAPromotionId() {
        PromoWinRequest request = PromoWinRequests.of("85");
        request.setPmid(null);

        PromoPayoutContext context = mapper.toInternal(request);

        assertThat(context.getVendorCampaignCode()).isNull();
    }

    /**
     * Bet-less types that legitimately belong on this endpoint but have no agreed classification yet.
     * Rejecting keeps them visible rather than misfiled.
     */
    @ParameterizedTest(name = "opt {0} is not yet classified")
    @ValueSource(strings = {"27", "72", "75", "76"})
    void rejectsTheOperationTypesStillAwaitingClassification(String opt) {
        assertThatThrownBy(() -> mapper.toInternal(PromoWinRequests.of(opt)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(opt);
    }

    @Test
    void rejectsAnUnrecognisedOperationType() {
        assertThatThrownBy(() -> mapper.toInternal(PromoWinRequests.of("999")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsAMissingOperationType() {
        assertThatThrownBy(() -> mapper.toInternal(PromoWinRequests.of(null)))
                .isInstanceOf(InvalidRequestException.class);
    }
}
