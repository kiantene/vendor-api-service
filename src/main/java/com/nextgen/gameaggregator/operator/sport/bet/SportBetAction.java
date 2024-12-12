package com.nextgen.gameaggregator.operator.sport.bet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.VendorCurrency;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.AgentApiCredentialService;
import com.nextgen.gameaggregator.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class SportBetAction {

    private final AgentApiCredentialService agentApiCredentialService;

    @Autowired
    public SportBetAction(AgentApiCredentialService agentApiCredentialService) {

        this.agentApiCredentialService = agentApiCredentialService;
    }

    public WalletRequest callToOperator(WalletRequest walletRequest, VendorCurrency vendorCurrency)
            throws InvalidAgentApiCredentialException, InvalidOperatorResponseException, InsufficientBalanceException {

        walletRequest.setRequestType(this.getClass().getSimpleName());
        final Integer INVALID_RESPONSE = ResponseCodes.Status.SC_INVALID_RESPONSE.code;
        Integer agentId = walletRequest.getAgentId();
        BigDecimal fromVendorRate = vendorCurrency.getFromVendorRate();
        BigDecimal toVendorRate = vendorCurrency.getToVendorRate();
        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        SportBetDto dto = new SportBetDto(walletRequest, fromVendorRate);

        try {
            String signature = AuthenticationService.generateSignatureWithJson(new ObjectMapper().writeValueAsString(dto), agentApiCredential.getApiSecret());

            walletRequest.setOperatorData(new ObjectMapper().writeValueAsString(dto));
            walletRequest.setOperatorEndpoint(apiUrl + EndPoints.SPORT_BET);
            walletRequest.setOperatorStart(System.currentTimeMillis());

            AtomicBoolean isTimeout = new AtomicBoolean(false);

            ResponseEntity<String> response = WebClient.create(apiUrl).post().uri(EndPoints.SPORT_BET)
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                    .onErrorResume(TimeoutException.class, e -> {
                        isTimeout.set(true);
                        return Mono.error(e);
                    })
                    .block();

            walletRequest.setOperatorEnd(System.currentTimeMillis());

            if (isTimeout.get()) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_OPERATOR_TIMEOUT.code);
            }

            if (response == null) throw new InvalidOperatorResponseException(INVALID_RESPONSE);

            HttpStatusCode httpStatusCode = response.getStatusCode();
            walletRequest.setOperatorHttpStatusCode(httpStatusCode.value());
            if (response.getStatusCode().isError()) {
                throw new InvalidOperatorResponseException(httpStatusCode.value());
            }

            String responseBody = response.getBody();
            walletRequest.setOperatorResponse(responseBody);
            WalletBalanceVo walletBalanceVo = new Gson().fromJson(responseBody, WalletBalanceVo.class);

            WalletRequestService.validateOperatorResponse(walletRequest, walletBalanceVo);

            BigDecimal balance = walletBalanceVo.getData().getBalance();

            boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
            if (isNegativeBalance) throw new InsufficientBalanceException();

            // Convert balance based on vendor's rate
            BigDecimal convertedBalance = balance.multiply(toVendorRate).stripTrailingZeros();
            walletRequest.setBalanceAfter(new BigDecimal(convertedBalance.toPlainString()));
            walletRequest.setOperatorResponseStatus(walletBalanceVo.getStatus());

        } catch (JsonSyntaxException jsonSyntaxException) { // map to InvalidOperatorResponseException
            throw new InvalidOperatorResponseException(INVALID_RESPONSE);
        } catch (InsufficientBalanceException | InvalidOperatorResponseException exception) {
            throw exception; // re-throw to caller
        } catch (Exception exception) {
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
        }

        return walletRequest;
    }
}
