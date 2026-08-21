package com.nextgen.gameaggregator.vendor.dotconnections.api.freespin;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.service.HttpService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the {@code freeSpinResult} → {@code PromoPayoutContext} mapping against a payload captured from
 * STG, so a field rename on either side fails here rather than in a live payout.
 */
class DotConnectionsFreeSpinResultRequestMapperTest {

    /** Verbatim from api-request-logs-stg 2026-08-11, /api/v1/dotconnections/freeSpinResult. */
    private static final String CAPTURED_PAYLOAD = """
            {"transaction_time":"2026-08-11T09:11:17Z","brand_uid":"18wn0cjb8q","currency":"TRY",
             "amount":9,"game_id":150000,"game_name":"6 Wild Sharks","round_id":"RG003-fs16172108",
             "wager_id":"RG003-54685242","provider":"relax","is_endround":true,"freespin_id":100900,
             "freespin_description":null,"brand_id":"S095136","sign":"2848A2A29A44E5E65A6126C7AA946B8F"}
            """;

    private final DotConnectionsFreeSpinResultRequestMapper mapper = new DotConnectionsFreeSpinResultRequestMapper();

    @Test
    void deserialisesEveryFieldThePayoutPathDependsOn() throws Exception {
        FreeSpinResultDto dto = HttpService.convertJsonToDto(CAPTURED_PAYLOAD, FreeSpinResultDto.class);

        // freespin_id was previously dropped by @JsonIgnoreProperties — the whole reason payout was unwireable
        assertThat(dto.getFreespinId()).isEqualTo(100900L);
        assertThat(dto.getBrandUid()).isEqualTo("18wn0cjb8q");
        assertThat(dto.getWagerId()).isEqualTo("RG003-54685242");
        assertThat(dto.getAmount()).isEqualByComparingTo("9");
        assertThat(dto.getCurrency()).isEqualTo("TRY");
        // game_id arrives as a JSON int, the DTO holds it as the String vendorGameCode
        assertThat(dto.getGameId()).isEqualTo("150000");
    }

    @Test
    void mapsGrantReferenceAndWagerLevelIdempotency() throws Exception {
        FreeSpinResultDto dto = HttpService.convertJsonToDto(CAPTURED_PAYLOAD, FreeSpinResultDto.class);

        PromoPayoutContext context = mapper.toInternal(dto);

        // resolved by (player, grant) — matched against CampaignPlayer.ext_info.vendorGrantRef
        assertThat(context.getVendorFreeRoundBonusId()).isEqualTo("100900");
        assertThat(context.getVendorPlayerUsername()).isEqualTo("18wn0cjb8q");

        // wager_id, not round_id: a round may settle across several wagers
        assertThat(context.getIdempotencyKey()).isEqualTo("RG003-54685242");
        assertThat(context.getVendorTransactionId()).isEqualTo("RG003-54685242");

        assertThat(context.getVendorPayoutAmount()).isEqualByComparingTo(new BigDecimal("9"));
        assertThat(context.getVendorCurrency()).isEqualTo("TRY");
        assertThat(context.getPromoType()).isEqualTo(PromoType.FREE_ROUND);

        // 2026-08-11T09:11:17Z
        assertThat(context.getVendorTransactionTime()).isEqualTo(1786439477000L);

        // never set for DCS: campaign create is LOCAL_ONLY, so there is no vendor-side campaign code
        assertThat(context.getVendorCampaignCode()).isNull();
    }

    @Test
    void leavesTransactionTimeNullWhenAbsentSoTheEnricherCanFallBack() throws Exception {
        String withoutTime = CAPTURED_PAYLOAD.replace("\"transaction_time\":\"2026-08-11T09:11:17Z\",", "");

        FreeSpinResultDto dto = HttpService.convertJsonToDto(withoutTime, FreeSpinResultDto.class);

        assertThat(dto.getTransactionTimeMillis()).isNull();
        assertThat(mapper.toInternal(dto).getVendorTransactionTime()).isNull();
    }
}
