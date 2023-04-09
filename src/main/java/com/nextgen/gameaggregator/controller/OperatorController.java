package com.nextgen.gameaggregator.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.repository.AgentApiCredentialRepository;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "testOperator/")
public class OperatorController {

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private AgentApiCredentialService agentApiCredentialService;

    @Autowired
    ControllerServices controllerServices;

    @Autowired
    private WalletService walletService;

    @Autowired
    AgentApiCredentialRepository agentApiCredentialRepository;

    @PostMapping(path = Endpoints.WALLET_BALANCE)
    public ResponseEntity<Map<String, Object>> walletBalance(@RequestBody ObjectNode json) {
        HashMap<String, Object> responseMap = new HashMap<>();
        try {

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(json.get("username").asText());
            if (gameSession != null) {
                Integer agentId = gameSession.getAgentId();
                AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
                controllerServices.clearAgentApiCredentials();
                agentApiCredential.setCallbackUrl(json.get("callbackUrl").asText());
                agentApiCredentialRepository.save(agentApiCredential);

                BigDecimal balance = walletService.getBalance(json.get("traceId").asText(), gameSession);

                responseMap.put("balance", balance);
            }

        } catch (Exception exception) {
            responseMap.put("exceptionName", exception.getClass().getSimpleName());
        }

        return new ResponseEntity<>(
                responseMap,
                HttpStatus.OK);

    }
}
