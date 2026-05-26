package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.gameaggregator.core.entity.GameSubcategory;
import com.nextgen.gameaggregator.core.entity.VendorGame;
import com.nextgen.gameaggregator.core.service.GameSubcategoryDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GameLaunchDataService {

    private final VendorGameDataService vendorGameDataService;
    private final GameSubcategoryDataService gameSubcategoryDataService;

    public GameSubcategoryInfo getBackfacingGameSubcategoryByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId) {
        VendorGame vendorGame = vendorGameDataService.getByVendorGameCodeAndVendorId(vendorGameCode, vendorId);

        GameSubcategory subcategory = gameSubcategoryDataService.get(vendorGame.getBackfacingGameSubCategoryId());

        return subcategory == null ? null : new GameSubcategoryInfo(subcategory.getId(), subcategory.getCode(), subcategory.getName());
    }
}
