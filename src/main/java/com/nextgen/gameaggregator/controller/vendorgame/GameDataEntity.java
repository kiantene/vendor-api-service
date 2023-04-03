package com.nextgen.gameaggregator.controller.vendorgame;

import com.nextgen.gameaggregator.entity.Platform;
import com.nextgen.gameaggregator.entity.VendorGame;
import com.nextgen.gameaggregator.entity.VendorGameCode;
import com.nextgen.gameaggregator.entity.VendorGameCurrency;
import lombok.Data;

import java.util.HashMap;

@Data
public class GameDataEntity {

    public VendorGame vendorGame = new VendorGame();
    public HashMap<String, VendorGameCode> vendorGameCodes = new HashMap<>();
    public HashMap<String, VendorGameCurrency> VendorGameCurrencies = new HashMap<>();

    public String defaultOpenGameCode = null;
    public String defaultBetGameCode = null;
//    public Boolean supportWeb = false;
//    public Boolean supportH5 = false;

    HashMap<String, Platform> platformSupported = new HashMap<>();
}
