package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.entity.ga.custom.WarehouseFutureEntity;
import com.nextgen.gameaggregator.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class WarehouseBetHistoryService {
    private final VendorGameService vendorGameService;
    private final VendorService vendorService;
    private final GameCategoryService gameCategoryService;
    private final CurrencyService currencyService;
    @Autowired
    public WarehouseBetHistoryService (VendorGameService vendorGameService, VendorService vendorService,
                                       GameCategoryService gameCategoryService, CurrencyService currencyService){
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
        this.gameCategoryService = gameCategoryService;
        this.currencyService = currencyService;

    }

    public void setWarehouseBetHistoryInfoCache(
            VendorGame vendorGame, Vendor vendor, GameCategory gameCategory, Currency currency) {

        CompletableFuture<VendorGame> futureVendorGame = CompletableFuture.supplyAsync(() -> {
            try {
                return vendorGameService.getByGameId(vendorGame.getId(), vendorGame);
            } catch (GameNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });


        CompletableFuture<Vendor> futureVendor = CompletableFuture.supplyAsync(() -> {
            try {
                return vendorService.getByVendorId(vendor.getId(), vendor);
            } catch (InvalidVendorException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Currency> futureCurrency = CompletableFuture.supplyAsync(() -> {
            try {
                return currencyService.getByCurrencyId(currency.getId(), currency);
            } catch (InvalidCurrencyException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<GameCategory> futureGameCategory = CompletableFuture.supplyAsync(() -> {
            try {
                return gameCategoryService.getByGameCategoryId(gameCategory.getId(), gameCategory);
            } catch (InvalidGameCategoryException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf
                (futureVendorGame, futureVendor, futureCurrency, futureGameCategory).join(); // Wait for all to complete
    }

    public WarehouseFutureEntity getWarehouseBetHistoryInfoCache(Integer vendorGameId, Integer vendorId, Integer gameCategoryId, Integer currencyId) {

        CompletableFuture<VendorGame> futureVendorGame = CompletableFuture.supplyAsync(() -> {
            try {
                return vendorGameService.getByGameId(vendorGameId, null);
            } catch (GameNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Vendor> futureVendor = CompletableFuture.supplyAsync(() -> {
            try {
                return vendorService.getByVendorId(vendorId, null);
            } catch (InvalidVendorException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Currency> futureCurrency = CompletableFuture.supplyAsync(() -> {
            try {
                return currencyService.getByCurrencyId(currencyId, null);
            } catch (InvalidCurrencyException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<GameCategory> futureGameCategory = CompletableFuture.supplyAsync(() -> {
            try {
                return gameCategoryService.getByGameCategoryId(gameCategoryId, null);
            } catch (InvalidGameCategoryException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(futureVendorGame, futureVendor, futureCurrency, futureGameCategory).join(); // Wait for all to complete

        WarehouseFutureEntity warehouseFutureEntity = new WarehouseFutureEntity();

        try {

            warehouseFutureEntity.setVendorGame(futureVendorGame.get());
            warehouseFutureEntity.setVendor(futureVendor.get());
            warehouseFutureEntity.setGameCategory(futureGameCategory.get());
            warehouseFutureEntity.setCurrency(futureCurrency.get());

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return warehouseFutureEntity;
    }


}
