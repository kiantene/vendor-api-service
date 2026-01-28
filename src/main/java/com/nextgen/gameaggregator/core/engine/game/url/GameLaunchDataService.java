package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.gameaggregator.core.entity.GameSubcategory;
import com.nextgen.gameaggregator.core.entity.VendorGame;
import com.nextgen.gameaggregator.core.service.GameSubcategoryCacheService;
import com.nextgen.gameaggregator.core.service.VendorGameCacheService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GameLaunchDataService {

    private final VendorGameCacheService vendorGameCacheService;
    private final GameSubcategoryCacheService gameSubcategoryCacheService;

    public String getBackfacingGameSubcategoryByVendorGameCodeAndVendorId(String vendorGameCode, Integer vendorId) {
        VendorGame vendorGame = vendorGameCacheService.getByVendorGameCodeAndVendorId(vendorGameCode, vendorId);

        GameSubcategory subcategory = gameSubcategoryCacheService.getById(vendorGame.getBackfacingGameSubCategoryId());

        return subcategory == null ? "" : subcategory.getName();
    }
}
