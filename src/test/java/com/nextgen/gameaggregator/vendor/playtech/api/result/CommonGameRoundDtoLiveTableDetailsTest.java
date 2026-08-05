package com.nextgen.gameaggregator.vendor.playtech.api.result;

import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.playtech.service.VendorService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers GA-14842 for the /GameResult (settle/round-close) endpoint: same liveTableDetails
 * gap as BetDto - CommonGameRoundDto must parse it, and the resolved vendorGameCode must match
 * what GameRoundGeneralAction now syncs the session to before doVerification() runs, or every
 * live-table round-close would mismatch against the session's stored (concatenated) vendorGameCode.
 */
class CommonGameRoundDtoLiveTableDetailsTest {

    private static final String LIVE_GAME_ROUND_REQUEST = """
            {
                "requestId": "9fecfe28-a9f3-45f6-96e7-a9190ea13787",
                "username": "ZTCNYA_4GU4E284B",
                "externalToken": "ZTCNYA_f26675fc-34e6-44e1-8a0b-6b48cfc206e8",
                "gameRoundCode": "live_87618250166#9ccb136e",
                "gameCodeName": "ubal",
                "pay": {
                    "transactionCode": "121056304",
                    "transactionDate": "2026-07-27 09:24:55.000",
                    "amount": "40",
                    "type": "WIN"
                },
                "liveTableDetails": {
                    "launchAlias": "bal_speedbac2",
                    "tableId": "12510",
                    "tableName": "UAT Speed Baccarat 2"
                }
            }
            """;

    private static final String SLOT_GAME_ROUND_REQUEST = """
            {
                "requestId": "c758f1d3-c941-4a19-b232-49af4a009879",
                "username": "SomePlayer6715",
                "externalToken": "25323079",
                "gameRoundCode": "39175543#3602117",
                "gameCodeName": "bj",
                "gameRoundClose": {
                    "date": "2018-04-13 10:55:40.000"
                }
            }
            """;

    @Test
    void liveGameRoundRequest_parsesLaunchAlias_andResolvesToGameCodeNameConcatenatedWithIt() throws Exception {
        CommonGameRoundDto dto = HttpService.convertJsonToDto(LIVE_GAME_ROUND_REQUEST, CommonGameRoundDto.class);

        assertThat(dto.getGameCodeName()).isEqualTo("ubal");
        assertThat(dto.getLiveTableDetails()).isNotNull();
        assertThat(dto.getLiveTableDetails().getLaunchAlias()).isEqualTo("bal_speedbac2");

        // this must match the vendorGameCode GameRoundGeneralAction synced the session to via
        // verifyAndRegenerateNewVendorGameCodeForGameSession(...) before doVerification() runs,
        // or the round-close would incorrectly fail as if the game had changed mid-session.
        String resolved = VendorService.resolveVendorGameCode(dto.getGameCodeName(), dto.getLiveTableDetails());
        assertThat(resolved).isEqualTo("ubal;bal_speedbac2");
    }

    @Test
    void slotGameRoundRequest_hasNoLiveTableDetails_resolvesToGameCodeNameDirectly() throws Exception {
        CommonGameRoundDto dto = HttpService.convertJsonToDto(SLOT_GAME_ROUND_REQUEST, CommonGameRoundDto.class);

        assertThat(dto.getGameCodeName()).isEqualTo("bj");
        assertThat(dto.getLiveTableDetails()).isNull();

        String resolved = VendorService.resolveVendorGameCode(dto.getGameCodeName(), dto.getLiveTableDetails());
        assertThat(resolved).isEqualTo("bj");
    }
}
