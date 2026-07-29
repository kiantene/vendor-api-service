package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.gameaggregator.core.entity.GameSubcategory;
import com.nextgen.gameaggregator.core.entity.VendorGame;
import com.nextgen.gameaggregator.core.service.GameSubcategoryDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers OAS-5003's null-safety fix: most vendor_games rows have no backfacing subcategory set
 * (null), and GameSubcategoryDataService.get(id) throws EntityNotFoundException rather than
 * returning null/empty for a missing id - so a null id must never reach it, or every such lookup
 * (the vast majority of games) would throw instead of resolving to an empty result.
 */
@ExtendWith(MockitoExtension.class)
class GameLaunchDataServiceTest {

    private static final String VENDOR_GAME_CODE = "top_games";
    private static final Integer VENDOR_ID = 63;

    @Mock
    private VendorGameDataService vendorGameDataService;
    @Mock
    private GameSubcategoryDataService gameSubcategoryDataService;
    @Mock
    private VendorGame vendorGame;

    private GameLaunchDataService gameLaunchDataService;

    @BeforeEach
    void setUp() {
        gameLaunchDataService = new GameLaunchDataService(vendorGameDataService, gameSubcategoryDataService);
        when(vendorGameDataService.getByVendorGameCodeAndVendorId(VENDOR_GAME_CODE, VENDOR_ID)).thenReturn(vendorGame);
    }

    @Test
    void whenBackfacingSubcategoryIdIsNull_returnsEmpty_withoutCallingGameSubcategoryDataService() {
        when(vendorGame.getBackfacingGameSubCategoryId()).thenReturn(null);

        var result = gameLaunchDataService.getBackfacingGameSubcategoryByVendorGameCodeAndVendorId(VENDOR_GAME_CODE, VENDOR_ID);

        assertThat(result).isEmpty();
        // GameSubcategoryDataService.get(null) throws EntityNotFoundException rather than
        // returning empty - it must never be called with a null id.
        verify(gameSubcategoryDataService, never()).get(null);
    }

    @Test
    void whenBackfacingSubcategoryIdPresent_resolvesGameSubcategoryInfo() {
        when(vendorGame.getBackfacingGameSubCategoryId()).thenReturn(111);

        GameSubcategory subcategory = mock(GameSubcategory.class);
        when(subcategory.getId()).thenReturn(111);
        when(subcategory.getCode()).thenReturn("category_lobby");
        when(subcategory.getName()).thenReturn("Category Lobby");
        when(gameSubcategoryDataService.get(111)).thenReturn(subcategory);

        var result = gameLaunchDataService.getBackfacingGameSubcategoryByVendorGameCodeAndVendorId(VENDOR_GAME_CODE, VENDOR_ID);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(111);
        assertThat(result.get().code()).isEqualTo("category_lobby");
        assertThat(result.get().name()).isEqualTo("Category Lobby");
    }
}
