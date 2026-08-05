package com.nextgen.gameaggregator.vendor.playtech.api.bet;

import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.playtech.service.VendorService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers GA-14842 against the two real vendor payload shapes: deserializes the exact request
 * bodies PlayTech sends (not hand-simplified stand-ins) and confirms
 * VendorService.resolveVendorGameCode() picks the right code for each.
 */
class BetDtoLiveTableDetailsTest {

    private static final String LIVE_GAME_REQUEST = """
            {
                "requestId": "9fecfe28-a9f3-45f6-96e7-a9190ea13787",
                "username": "ZTCNYA_4GU4E284B",
                "externalToken": "ZTCNYA_f26675fc-34e6-44e1-8a0b-6b48cfc206e8",
                "gameRoundCode": "live_87618250166#9ccb136e",
                "transactionCode": "121056303",
                "transactionDate": "2026-07-27 09:24:51.000",
                "amount": "20",
                "internalFundChanges": [],
                "gameCodeName": "ubal",
                "betDetails": {
                    "tableCoverage": 45,
                    "betType": "MAIN_BET"
                },
                "liveTableDetails": {
                    "launchAlias": "bal_speedbac2",
                    "tableId": "12510",
                    "tableName": "UAT Speed Baccarat 2"
                }
            }
            """;

    private static final String SLOT_GAME_REQUEST = """
            {
              "requestId":"c758f1d3-c941-4a19-b232-49af4a009879",
              "username":"SomePlayer6715",
              "externalToken":"25323079",
              "gameRoundCode":"39175543#3602117",
              "transactionCode":"61003870",
              "transactionDate":"2018-04-13 10:55:40.000",
              "amount":"25",
              "internalFundChanges":[],
              "gameCodeName":"bj"
            }
            """;

    @Test
    void liveGameRequest_parsesLaunchAlias_andResolvesToGameCodeNameConcatenatedWithIt() throws Exception {
        BetDto dto = HttpService.convertJsonToDto(LIVE_GAME_REQUEST, BetDto.class);

        assertThat(dto.getGameCodeName()).isEqualTo("ubal");
        assertThat(dto.getLiveTableDetails()).isNotNull();
        assertThat(dto.getLiveTableDetails().getLaunchAlias()).isEqualTo("bal_speedbac2");
        assertThat(dto.getLiveTableDetails().getTableId()).isEqualTo("12510");
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("20"));

        // "ubal" and "bal_speedbac2" share no common prefix - neither alone is the stored
        // vendorGameCode, so both are concatenated as "gameCodeName;launchAlias".
        String resolved = VendorService.resolveVendorGameCode(dto.getGameCodeName(), dto.getLiveTableDetails());
        assertThat(resolved).isEqualTo("ubal;bal_speedbac2");
    }

    @Test
    void slotGameRequest_hasNoLiveTableDetails_resolvesToGameCodeNameDirectly() throws Exception {
        BetDto dto = HttpService.convertJsonToDto(SLOT_GAME_REQUEST, BetDto.class);

        assertThat(dto.getGameCodeName()).isEqualTo("bj");
        assertThat(dto.getLiveTableDetails()).isNull();
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("25"));

        String resolved = VendorService.resolveVendorGameCode(dto.getGameCodeName(), dto.getLiveTableDetails());
        assertThat(resolved).isEqualTo("bj");
    }
}
