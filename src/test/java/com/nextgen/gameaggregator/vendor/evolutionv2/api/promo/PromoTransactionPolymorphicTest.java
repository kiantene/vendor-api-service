package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evolution v2 promo-payout integration.
 *
 * <p>Verifies that {@code promoTransaction} deserializes polymorphically on {@code type}, and that
 * {@code campaignId} is captured for both vendor representations (numeric for Red Tiger, string for
 * Livespins) and normalized via {@link PromoTransactionDto#resolveCampaignId()}.</p>
 */
class PromoTransactionPolymorphicTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void rewardGameType_deserializesToBase_withNoCampaignId() throws Exception {
        String json = """
                {
                  "type": "RewardGamePlayableSpent",
                  "id": "TD1459zzz",
                  "amount": 1.556179,
                  "voucherId": "d82ce074-bd5a-11eb-8529-0242ac130003",
                  "playableBalance": 0.000000
                }""";

        PromoTransactionDto tx = mapper.readValue(json, PromoTransactionDto.class);

        assertThat(tx).isExactlyInstanceOf(PromoTransactionDto.class);
        assertThat(tx.getVoucherId()).isEqualTo("d82ce074-bd5a-11eb-8529-0242ac130003");
        assertThat(tx.getPlayableBalance()).isEqualByComparingTo("0");
        assertThat(tx.resolveCampaignId()).isNull();
    }

    @Test
    void smartTournamentType_deserializesToRedTiger_withNumericCampaignId() throws Exception {
        String json = """
                {
                  "type": "SmartTournamentMonetaryReward",
                  "id": "TD1459zzz",
                  "amount": 1.55,
                  "instanceCode": null,
                  "instanceId": null,
                  "campaignCode": "campaign_code",
                  "campaignId": 456
                }""";

        PromoTransactionDto tx = mapper.readValue(json, PromoTransactionDto.class);

        assertThat(tx).isExactlyInstanceOf(RedTigerPromoTransactionDto.class);
        assertThat(((RedTigerPromoTransactionDto) tx).getCampaignId()).isEqualTo(456);
        assertThat(tx.getCampaignCode()).isEqualTo("campaign_code");
        assertThat(tx.resolveCampaignId()).isEqualTo("456");
    }

    @Test
    void smartSpinsType_deserializesToRedTiger() throws Exception {
        String json = """
                {
                  "type": "SmartSpinsMonetaryReward",
                  "id": "TD1459zzz",
                  "amount": 1.55,
                  "instanceCode": "instance_code",
                  "instanceId": 123,
                  "campaignId": 789
                }""";

        PromoTransactionDto tx = mapper.readValue(json, PromoTransactionDto.class);

        assertThat(tx).isExactlyInstanceOf(RedTigerPromoTransactionDto.class);
        assertThat(tx.getInstanceId()).isEqualTo(123);
        assertThat(tx.resolveCampaignId()).isEqualTo("789");
    }

    @Test
    void cashRewardType_deserializesToLivespins_withStringCampaignId() throws Exception {
        String json = """
                {
                  "id": "111354583321722855",
                  "amount": 6.0,
                  "campaignId": "1",
                  "reason": "cash_reward_reason_description",
                  "type": "CashReward"
                }""";

        PromoTransactionDto tx = mapper.readValue(json, PromoTransactionDto.class);

        assertThat(tx).isExactlyInstanceOf(LivespinsPromoTransactionDto.class);
        assertThat(((LivespinsPromoTransactionDto) tx).getCampaignId()).isEqualTo("1");
        assertThat(tx.getReason()).isEqualTo("cash_reward_reason_description");
        assertThat(tx.resolveCampaignId()).isEqualTo("1");
    }

    @Test
    void jackpotWinType_deserializesJackpotsArray() throws Exception {
        String json = """
                {
                  "type": "JackpotWin",
                  "id": "9AotBIvi23",
                  "amount": 350.762048,
                  "jackpots": [
                    { "id": "444041xyz", "winAmount": 325.118042 },
                    { "id": "555739abc", "winAmount": 25.644006 }
                  ]
                }""";

        PromoTransactionDto tx = mapper.readValue(json, PromoTransactionDto.class);

        assertThat(tx).isExactlyInstanceOf(PromoTransactionDto.class);
        assertThat(tx.getJackpots()).hasSize(2);
        assertThat(tx.getJackpots().get(0).getId()).isEqualTo("444041xyz");
        assertThat(tx.getJackpots().get(0).getWinAmount()).isEqualByComparingTo(new BigDecimal("325.118042"));
        assertThat(tx.resolveCampaignId()).isNull();
    }

    @Test
    void type_remainsVisibleAfterPolymorphicDeserialization() throws Exception {
        String json = """
                { "type": "CashReward", "id": "x", "amount": 1, "campaignId": "abc" }""";

        PromoTransactionDto tx = mapper.readValue(json, PromoTransactionDto.class);

        assertThat(tx.getType()).isEqualTo("CashReward");
    }
}
