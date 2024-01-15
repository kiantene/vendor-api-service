package com.nextgen.gameaggregator.controller.agent;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.controller.ControllerServices;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.repository.ga.writer.AgentApiCredentialRepository;
import com.nextgen.gameaggregator.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    RequestService requestService;
    @Autowired
    AgentApiCredentialRepository agentApiCredentialRepository;

    @Autowired
    ControllerServices controllerServices;

    //for QA use to test disable agent credential status only
    @PostMapping(path = "/status")
    public ResponseEntity<Map<String, String>> status(@RequestBody ObjectNode json) {
        HashMap<String, String> responseMap = new HashMap<>();
        if (requestService.isTestEnvironment(profilesActive)) {
            AgentApiCredential agentApiCredential = agentApiCredentialRepository.findByAgentId(Integer.parseInt(json.get("id").toString()));
            controllerServices.clearAgentApiCredentials();

            if (agentApiCredential != null) {
                agentApiCredential.setStatus(Integer.parseInt(json.get("status").toString()));
                agentApiCredentialRepository.save(agentApiCredential);
                responseMap.put("status", "Success");
                responseMap.put("id", json.get("id").toString());
                return new ResponseEntity<>(
                        responseMap,
                        HttpStatus.OK);
            }

            responseMap.put("status", "fail, Agent credential line not found");
            responseMap.put("id", json.get("id").toString());
        } else {
            responseMap.put("status", "Invalid environment, only support staging and qa");
        }
        return new ResponseEntity<>(
                responseMap,
                HttpStatus.BAD_REQUEST);


    }

    //for QA use to update invalid agent api callback url for test failure only
    @PostMapping(path = "/callbackUrl")
    public ResponseEntity<Map<String, String>> callbackUrl(@RequestBody ObjectNode json) {
        HashMap<String, String> responseMap = new HashMap<>();
        if (requestService.isTestEnvironment(profilesActive)) {
            AgentApiCredential agentApiCredential = agentApiCredentialRepository.findByAgentId(Integer.parseInt(json.get("id").toString()));
            controllerServices.clearAgentApiCredentials();

            if (agentApiCredential != null) {
                agentApiCredential.setCallbackUrl(json.get("callbackUrl").asText());
                agentApiCredentialRepository.save(agentApiCredential);
                responseMap.put("status", "Success");
                responseMap.put("id", json.get("id").toString());
                return new ResponseEntity<>(
                        responseMap,
                        HttpStatus.OK);
            }

            responseMap.put("status", "fail, Agent credential line not found");
            responseMap.put("id", json.get("id").toString());

        } else {
            responseMap.put("status", "Invalid environment, only support staging and qa");
        }

        return new ResponseEntity<>(
                responseMap,
                HttpStatus.BAD_REQUEST);

    }
}
