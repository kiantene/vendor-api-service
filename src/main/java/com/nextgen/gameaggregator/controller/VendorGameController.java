package com.nextgen.gameaggregator.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.VendorGame;
import com.nextgen.gameaggregator.repository.VendorGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "vendorGame/")
public class VendorGameController {

    @Autowired
    VendorGameRepository vendorGameRepository;

    @PostMapping(path = "/status")
    public ResponseEntity<Map<String, String>> status(@RequestBody ObjectNode json){
        HashMap<String, String> responseMap = new HashMap<>();

        VendorGame vendorGame = vendorGameRepository.findByVendorGameCode(json.get("vendorGameCode").asText());
        if(vendorGame !=null){
            vendorGame.setStatus(Integer.parseInt(json.get("status").toString()));
            vendorGameRepository.save(vendorGame);
            responseMap.put("status", "Success");
            responseMap.put("id", json.get("vendorGameCode").asText());
            return new ResponseEntity<>(
                    responseMap ,
                    HttpStatus.OK);
        }

        responseMap.put("status", "fail, vendor game not found");
        responseMap.put("id", json.get("vendorGameCode").asText());

        return new ResponseEntity<>(
                responseMap ,
                HttpStatus.BAD_REQUEST);


    }
}