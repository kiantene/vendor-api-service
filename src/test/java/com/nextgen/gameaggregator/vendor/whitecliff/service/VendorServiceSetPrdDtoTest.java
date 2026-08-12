package com.nextgen.gameaggregator.vendor.whitecliff.service;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl.PrdDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers OAS-5003: WCLive category/lobby launches (e.g. "top_games", "baccarat") were being sent
 * as prd.table_id, which the vendor rejects with "Table with id top_games does not exist" since
 * table_id is meant for a specific table (Evolution-style), not a category/lobby code. Rows tagged
 * with subcategory code "WCLIVE_LOBBY" in vendor_games must be sent as prd.category instead -
 * routes on the semantic code, not the subcategory's auto-increment id (fragile across envs).
 */
class VendorServiceSetPrdDtoTest {

    private static final Integer LIVE_CATEGORY_ID = 5;
    private static final Integer CATEGORY_LOBBY_SUBCATEGORY_ID = 111;

    private GameSession buildLiveGameSession(String vendorGameCode) {
        GameSession gameSession = new GameSession();
        gameSession.setGameCategoryId(LIVE_CATEGORY_ID);
        gameSession.setVendorGameCode(vendorGameCode);
        gameSession.setVendorPlatformCode("WEB");
        return gameSession;
    }

    private GameLaunchDataService.GameSubcategoryInfo categoryLobbySubcategory() {
        return new GameLaunchDataService.GameSubcategoryInfo(CATEGORY_LOBBY_SUBCATEGORY_ID, "WCLIVE_LOBBY", "WCLIVE Lobby");
    }

    @Test
    void categoryLobbyGame_sendsCategory_notTableId() throws InvalidFormatException {
        GameSession gameSession = buildLiveGameSession("top_games");

        PrdDto prdDto = VendorService.setPrdDto(gameSession, "1", categoryLobbySubcategory());

        assertThat(prdDto.getCategory()).isEqualTo("top_games");
        assertThat(prdDto.getTable_id()).isNull();
    }

    @Test
    void categoryLobbySubcategory_routesByCode_notById() throws InvalidFormatException {
        // Same semantic code, a different (env-specific) auto-increment id - must still route
        // to category, since the id is fragile across environments and the code is not.
        GameSession gameSession = buildLiveGameSession("top_games");
        GameLaunchDataService.GameSubcategoryInfo differentIdSameCode =
                new GameLaunchDataService.GameSubcategoryInfo(999, "WCLIVE_LOBBY", "WCLIVE Lobby");

        PrdDto prdDto = VendorService.setPrdDto(gameSession, "1", differentIdSameCode);

        assertThat(prdDto.getCategory()).isEqualTo("top_games");
        assertThat(prdDto.getTable_id()).isNull();
    }

    @Test
    void specificTableGame_noSubcategoryInfo_stillSendsTableId() throws InvalidFormatException {
        GameSession gameSession = buildLiveGameSession("baccarat0001");

        PrdDto prdDto = VendorService.setPrdDto(gameSession, "1", null);

        assertThat(prdDto.getTable_id()).isEqualTo("baccarat0001");
        assertThat(prdDto.getCategory()).isNull();
    }

    @Test
    void specificTableGame_subcategoryPresentButNotTheLobbyOne_stillSendsTableId() throws InvalidFormatException {
        GameSession gameSession = buildLiveGameSession("baccarat0001");
        GameLaunchDataService.GameSubcategoryInfo unrelatedSubcategory =
                new GameLaunchDataService.GameSubcategoryInfo(42, "some_other", "Some Other Subcategory");

        PrdDto prdDto = VendorService.setPrdDto(gameSession, "1", unrelatedSubcategory);

        assertThat(prdDto.getTable_id()).isEqualTo("baccarat0001");
        assertThat(prdDto.getCategory()).isNull();
    }

    @Test
    void lobbyCodeZero_ignoresSubcategory_usesTypeInstead() throws InvalidFormatException {
        GameSession gameSession = buildLiveGameSession("0");

        PrdDto prdDto = VendorService.setPrdDto(gameSession, "1", categoryLobbySubcategory());

        assertThat(prdDto.getType()).isEqualTo(0);
        assertThat(prdDto.getCategory()).isNull();
        assertThat(prdDto.getTable_id()).isNull();
    }

    @Test
    void nonLiveGame_usesTypeRegardlessOfSubcategory() throws InvalidFormatException {
        GameSession gameSession = new GameSession();
        gameSession.setGameCategoryId(1);
        gameSession.setVendorGameCode("7");
        gameSession.setVendorPlatformCode("H5");

        PrdDto prdDto = VendorService.setPrdDto(gameSession, "1", categoryLobbySubcategory());

        assertThat(prdDto.getType()).isEqualTo(7);
        assertThat(prdDto.getCategory()).isNull();
        assertThat(prdDto.getTable_id()).isNull();
    }

    @Test
    void isMobile_setFromH5PlatformCode() throws InvalidFormatException {
        GameSession gameSession = buildLiveGameSession("top_games");
        gameSession.setVendorPlatformCode("H5");

        PrdDto prdDto = VendorService.setPrdDto(gameSession, "1", categoryLobbySubcategory());

        assertThat(prdDto.getIs_mobile()).isTrue();
    }
}
