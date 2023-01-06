package com.nextgen.gameaggregator.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.repository.AgentApiCredentialRepository;
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
@RequestMapping(path = "agentApiCredential/")
public class AgentApiCredentialController {

    @Autowired
    AgentApiCredentialRepository agentApiCredentialRepository;

    @PostMapping(path = "/status")
    public ResponseEntity<Map<String, String>> status(@RequestBody ObjectNode json){
        HashMap<String, String> responseMap = new HashMap<>();

        AgentApiCredential agentApiCredential = agentApiCredentialRepository.findByAgentId(Integer.parseInt(json.get("id").toString()));

        if(agentApiCredential !=null){
            agentApiCredential.setStatus(Integer.parseInt(json.get("status").toString()));
            agentApiCredentialRepository.save(agentApiCredential);
            responseMap.put("status", "Success");
            responseMap.put("id", json.get("id").toString());
            return new ResponseEntity<>(
                    responseMap ,
                    HttpStatus.OK);
        }

        responseMap.put("status", "fail, Agent credential line not found");
        responseMap.put("id", json.get("id").toString());

        return new ResponseEntity<>(
                responseMap ,
                HttpStatus.BAD_REQUEST);


    }
}
