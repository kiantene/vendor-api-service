package com.nextgen.gameaggregator.controller;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.AgentPlayer;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.repository.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.VendorPlayerRepository;
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
@RequestMapping(path = "agentPlayer/")
public class AgentPlayerController {

    @Autowired
    AgentPlayerRepository agentPlayerRepository;

    @Autowired
    VendorPlayerRepository vendorPlayerRepository;

    @PostMapping(path = "/status")
    public ResponseEntity<Map<String, String>> status(@RequestBody ObjectNode json){
        HashMap<String, String> responseMap = new HashMap<>();
        responseMap.put("status", "Success");
        responseMap.put("username", json.get("username").toString());
        responseMap.put("status", json.get("status").toString());

        VendorPlayer vendorPlayer = vendorPlayerRepository.findByUsername(json.get("username").asText());


        if(vendorPlayer!=null){
            AgentPlayer agentPlayer = agentPlayerRepository.findById(vendorPlayer.getId()).orElse(null);

            if(agentPlayer !=null){
                agentPlayer.setStatus(Integer.parseInt(json.get("status").toString()));
                agentPlayerRepository.save(agentPlayer);

                responseMap.put("status", "Success");
                responseMap.put("username", json.get("username").toString());
                return new ResponseEntity<>(
                        responseMap ,
                        HttpStatus.OK);
            }

        }

        responseMap.put("status", "fail, username not found");
        responseMap.put("username", json.get("username").asText());

        return new ResponseEntity<>(
                responseMap ,
                HttpStatus.BAD_REQUEST);


    }
}
