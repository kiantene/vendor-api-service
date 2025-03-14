package com.nextgen.gameaggregator.controller.agent;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.controller.ControllerServices;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.ga.writer.RawGameSessionRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorPlayerRepository;
import com.nextgen.gameaggregator.service.RequestService;
import lombok.Data;
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
@RequestMapping(path = "agentPlayer/")
public class AgentPlayerController {

    @Autowired
    RequestService requestService;
    @Autowired
    AgentPlayerRepository agentPlayerRepository;

    @Autowired
    VendorPlayerRepository vendorPlayerRepository;

    @Autowired
    RawGameSessionRepository rawGameSessionRepository;

    @Autowired
    ControllerServices controllerServices;

    //for QA use to test disable agent player status
    @PostMapping(path = "/status")
    public ResponseEntity<Map<String, String>> status(@RequestBody ObjectNode json) {
        HashMap<String, String> responseMap = new HashMap<>();
        if (requestService.isTestEnvironment()) {
            responseMap.put("status", "Success");
            responseMap.put("username", json.get("username").toString());
            responseMap.put("status", json.get("status").toString());

            VendorPlayer vendorPlayer = vendorPlayerRepository.findByUsername(json.get("username").asText());
            controllerServices.clearAgentPlayers();


            if (vendorPlayer != null) {
                AgentPlayer agentPlayer = agentPlayerRepository.findById(vendorPlayer.getAgentPlayerId()).orElse(null);

                if (agentPlayer != null) {
                    agentPlayer.setStatus(Integer.parseInt(json.get("status").toString()));
                    agentPlayerRepository.save(agentPlayer);

                    responseMap.put("status", "Success");
                    responseMap.put("username", json.get("username").asText());
                    return new ResponseEntity<>(
                            responseMap,
                            HttpStatus.OK);
                }

            }

            responseMap.put("status", "fail, username not found");
            responseMap.put("username", json.get("username").asText());
        } else {
            responseMap.put("status", "Invalid environment, only support staging and qa");
        }
        return new ResponseEntity<>(
                responseMap,
                HttpStatus.BAD_REQUEST);


    }

    @PostMapping(path = "/gameSession")
    public ResponseEntity<Detailvo> gameSession(@RequestBody ObjectNode json) {
        Detailvo detailvo = new Detailvo();
        if (requestService.isTestEnvironment()) {
            detailvo.setGameSession(rawGameSessionRepository.findByToken(json.get("token").asText()));
            return new ResponseEntity<>(
                    detailvo,
                    HttpStatus.OK);
        } else {
            return new ResponseEntity<>(
                    detailvo,
                    HttpStatus.BAD_REQUEST);
        }


    }

    @Data
    static class Detailvo {

        public GameSession gameSession;
    }
}
