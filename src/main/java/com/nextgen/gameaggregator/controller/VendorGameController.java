package com.nextgen.gameaggregator.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "vendorGame/")
public class VendorGameController {

    @Autowired
    VendorGameRepository vendorGameRepository;
    @Autowired
    CurrencyRepository currencyRepository;
    @Autowired
    LanguageRepository languageRepository;

    @Autowired
    VendorGameCurrencyRepository vendorGameCurrencyRepository;

    @Autowired
    VendorGameCodeRepository vendorGameCodeRepository;

    @Autowired
    ControllerServices controllerServices;

    @PostMapping(path = "status")
    public ResponseEntity<Map<String, String>> status(@RequestBody ObjectNode json) {
        HashMap<String, String> responseMap = new HashMap<>();

        VendorGame vendorGame = vendorGameRepository.findByCode(json.get("gameCode").asText());


        if (vendorGame == null) {
            responseMap.put("status", "fail, vendor game not found");
            responseMap.put("id", json.get("gameCode").asText());
            return new ResponseEntity<>(responseMap, HttpStatus.BAD_REQUEST);
        }

        List<VendorGameCurrency> vendorGameCurrencyList = vendorGameCurrencyRepository.findByVendorGameId(vendorGame.getId());

        for (VendorGameCurrency vendorGameCurrency : vendorGameCurrencyList) {
            vendorGameCurrency.setStatus(Integer.parseInt(json.get("status").toString()));
            vendorGameCurrencyRepository.save(vendorGameCurrency);
        }

        List<VendorGameCode> vendorGameCodeList = vendorGameCodeRepository.findByVendorGameId(vendorGame.getId());
        for (VendorGameCode vendorGameCode : vendorGameCodeList) {
            vendorGameCode.setStatus(Integer.parseInt(json.get("status").toString()));
            vendorGameCodeRepository.save(vendorGameCode);
        }


        vendorGame.setStatus(Integer.parseInt(json.get("status").toString()));
        vendorGameRepository.save(vendorGame);
        controllerServices.clearVendorGames();
        responseMap.put("status", "Success");

        return new ResponseEntity<>(responseMap, HttpStatus.OK);





    }

    @PostMapping(path = "statusCurrency")
    public ResponseEntity<Map<String, String>> statusCurrency(@RequestBody ObjectNode json) {
        HashMap<String, String> responseMap = new HashMap<>();

        VendorGame vendorGame = vendorGameRepository.findByCode(json.get("gameCode").asText());


        if (vendorGame == null) {
            responseMap.put("status", "fail, vendor game not found");
            responseMap.put("id", json.get("gameCode").asText());
            return new ResponseEntity<>(responseMap, HttpStatus.BAD_REQUEST);
        }

        Currency currency = currencyRepository.findByCode(json.get("currency").asText());
        if (currency == null) {
            responseMap.put("status", "fail, currency not found");
            responseMap.put("id", json.get("gameCode").asText().toUpperCase());
            return new ResponseEntity<>(responseMap, HttpStatus.BAD_REQUEST);
        }

        VendorGameCurrency vendorGameCurrency = vendorGameCurrencyRepository.findByVendorGameIdAndCurrencyId(vendorGame.getId(), currency.getId());

        if (vendorGameCurrency == null) {
            responseMap.put("status", "fail, game not supported currency");
            responseMap.put("id", json.get("gameCode").asText().toUpperCase());
            return new ResponseEntity<>(responseMap, HttpStatus.BAD_REQUEST);
        }

        vendorGameCurrency.setStatus(Integer.parseInt(json.get("status").toString()));
        vendorGameCurrencyRepository.save(vendorGameCurrency);
        responseMap.put("status", "Success");

        return new ResponseEntity<>(responseMap, HttpStatus.OK);

    }

    @PostMapping(path = "statusLanguage")
    public ResponseEntity<Map<String, String>> statusLanguage(@RequestBody ObjectNode json) {
        HashMap<String, String> responseMap = new HashMap<>();

        VendorGame vendorGame = vendorGameRepository.findByCode(json.get("gameCode").asText());


        if (vendorGame == null) {
            responseMap.put("status", "fail, vendor game not found");
            responseMap.put("id", json.get("gameCode").asText());
            return new ResponseEntity<>(responseMap, HttpStatus.BAD_REQUEST);
        }

        Language language = languageRepository.findByCode(json.get("language").asText());
        if (language == null) {
            responseMap.put("status", "fail, language not found");
            responseMap.put("id", json.get("gameCode").asText().toUpperCase());
            return new ResponseEntity<>(responseMap, HttpStatus.BAD_REQUEST);
        }

        List<VendorGameCode>  vendorGameCodeList = vendorGameCodeRepository.findByVendorGameIdAndLanguageId(vendorGame.getId(), language.getId());

        for (VendorGameCode vendorGameCode : vendorGameCodeList) {
            vendorGameCode.setStatus(Integer.parseInt(json.get("status").toString()));
            vendorGameCodeRepository.save(vendorGameCode);
        }
        responseMap.put("status", "Success");
        return new ResponseEntity<>(responseMap, HttpStatus.OK);

    }
}
