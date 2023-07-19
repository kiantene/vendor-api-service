package com.nextgen.gameaggregator.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetAction;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetDto;
import com.nextgen.gameaggregator.repository.AgentApiCredentialRepository;
import com.nextgen.gameaggregator.service.*;
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
    private WalletBetAction walletBetAction;

    @Autowired
    AgentApiCredentialRepository agentApiCredentialRepository;

    @Autowired
    private AuthenticationService authenticationService;


    @PostMapping(path = EndPoints.WALLET_BALANCE)
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

    @PostMapping(path = EndPoints.WALLET_BET)
    public ResponseEntity<Map<String, Object>> walletBet(@RequestBody ObjectNode json) {
        HashMap<String, Object> responseMap = new HashMap<>();
        try {

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(json.get("username").asText());

            if (gameSession != null) {
                Integer agentId = gameSession.getAgentId();
                AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
                controllerServices.clearAgentApiCredentials();
                agentApiCredential.setCallbackUrl(json.get("callbackUrl").asText());
                agentApiCredentialRepository.save(agentApiCredential);

                //BigDecimal balance = walletService.getBalance(json.get("traceId").asText(), rawGameSession);

//                responseMap.put("balance", balance);


                String test = "{\"traceId\":\"d541e5cd-5306-4efe-83a2-830cea4cd614\",\"username\":\"100125\",\"transactionId\":\"d541e5cd-5306-4efe-83a2-830cea4cd614\",\"externalTransactionId\":\"1645311546127716352\",\"amount\":5,\"currency\":\"CNY\",\"token\":\"2979a267-c0bd-4ad7-8684-038e800db53e\",\"gameCode\":\"PGS_100\",\"roundId\":\"1645311546127716352\",\"timestamp\":1681107814755}";
                WalletBetDto walletBetDto = HttpService.convertJsonToDto(test, WalletBetDto.class);

//                WalletBetDto walletBetDto = new WalletBetDto();
//                walletBetDto.setTraceId(String.valueOf(UUID.randomUUID()));
//                walletBetDto.setTransactionId(String.valueOf(UUID.randomUUID()));
//                walletBetDto.setUsername(rawGameSession.getAgentPlayerUsername());
//                walletBetDto.setCurrency(rawGameSession.getCurrencyCode());
//                walletBetDto.setToken(rawGameSession.getToken());
//                walletBetDto.setExternalTransactionId(String.valueOf(UUID.randomUUID()));
//                walletBetDto.setAmount(BigDecimal.valueOf(5));
//                walletBetDto.setGameCode(rawGameSession.getGameCode());
//                walletBetDto.setRoundId(String.valueOf(UUID.randomUUID()));
//                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//                walletBetDto.setTimestamp(timestamp.getTime());

                String signature = authenticationService.generateSignature(walletBetDto, "8c6450bce62aee29a530da1020dc8f6c19a4e4599a0941bb96839a765d03e5ec");

//                WalletBalanceVo balanceVo = walletBetAction.call(agentApiCredential, walletBetDto);

            }


        } catch (Exception exception) {
            responseMap.put("exceptionName", exception.getClass().getSimpleName());
        }

        return new ResponseEntity<>(
                responseMap,
                HttpStatus.OK);
    }
}
