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

    public Integer getBackfacingGameSubcategoryIdByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId) {
        GameSubcategory subcategory = getBackfacingGameSubcategory(vendorGameCode, vendorId);

        return subcategory == null ? 0 : subcategory.getId();
    }

    public String getBackfacingGameSubcategoryCodeByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId) {
        GameSubcategory subcategory = getBackfacingGameSubcategory(vendorGameCode, vendorId);

        return subcategory == null ? "" : subcategory.getCode();
    }

    public String getBackfacingGameSubcategoryNameByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId) {
        GameSubcategory subcategory = getBackfacingGameSubcategory(vendorGameCode, vendorId);

        return subcategory == null ? "" : subcategory.getName();
    }

    private GameSubcategory getBackfacingGameSubcategory(String vendorGameCode, Integer vendorId) {
        VendorGame vendorGame = vendorGameDataService.getByVendorGameCodeAndVendorId(vendorGameCode, vendorId);

        return gameSubcategoryDataService.get(vendorGame.getBackfacingGameSubCategoryId());
    }
}
