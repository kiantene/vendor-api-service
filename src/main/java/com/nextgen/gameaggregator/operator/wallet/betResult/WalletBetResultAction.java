package com.nextgen.gameaggregator.operator.wallet.betResult;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorLogVo;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.AuthenticationService;
import com.nextgen.gameaggregator.service.OperatorRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WalletBetResultAction {
    @Value("${testing.stub:false}")
    private Boolean useStub;

    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    OperatorRequestService operatorRequestService;

    @Autowired
    AuthenticationService authenticationService;

    public WalletBalanceVo call(AgentApiCredential agentApiCredential, WalletBetResultDto dto) throws InvalidOperatorResponseException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return operatorRequestService.responseOperatorSub();
        }
        WalletBalanceVo responseVo = null;

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        log.info("walletBetResultDto : " + dto);

        ResponseEntity apiResponse = WebClient.create(agentApiCredential.getCallbackUrl())
                .post()
                .uri(Endpoints.WALLET_BET_RESULT)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(Endpoints.HEADER_SIGNATURE, signature)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatus.BAD_REQUEST::equals, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        OperatorLogVo operatorLogVo = operatorRequestService.createOperatorLogVo(
                Endpoints.WALLET_BET, agentApiCredential.getCallbackUrl(), dto, apiResponse, signature, profilesActive);


        try {
            // 1. validate HTTP Response Code
            operatorRequestService.validateOperatorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), WalletBalanceVo.class);
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            operatorRequestService.validateResponse(responseVo);

            //3. validate username and currency
            operatorRequestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            operatorRequestService.operatorStatusException(responseVo.getStatus());


        } catch (HttpResponseStatusCodeException httpResponseStatusCodeException) {
            operatorRequestService.failResponseLog(operatorLogVo, httpResponseStatusCodeException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (JsonSyntaxException jsonSyntaxException) {
            operatorRequestService.failResponseLog(operatorLogVo, jsonSyntaxException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidTokenException invalidTokenException) {
            operatorRequestService.failResponseLog(operatorLogVo, invalidTokenException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_TOKEN.code);

        } catch (InvalidSignatureException invalidSignatureException) {
            operatorRequestService.failResponseLog(operatorLogVo, invalidSignatureException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_SIGNATURE.code);

        } catch (InvalidPlayerException invalidPlayerException) {
            operatorRequestService.failResponseLog(operatorLogVo, invalidPlayerException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_USER_NOT_EXISTS.code);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            operatorRequestService.failResponseLog(operatorLogVo, disabledAgentPlayerException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (GameNotSupportedException gameNotSupportedException) {
            operatorRequestService.failResponseLog(operatorLogVo, gameNotSupportedException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            operatorRequestService.failResponseLog(operatorLogVo, insufficientBalanceException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidRequestException invalidRequestException) {
            operatorRequestService.failResponseLog(operatorLogVo, invalidRequestException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_REQUEST.code);

        } catch (BetNotFoundException betNotFoundException) {
            operatorRequestService.failResponseLog(operatorLogVo, betNotFoundException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS.code);

        } catch (SystemMaintenanceException systemMaintenanceException) {
            operatorRequestService.failResponseLog(operatorLogVo, systemMaintenanceException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNDER_MAINTENANCE.code);

        } catch (DuplicateTransactionException duplicateTransactionException) {
            operatorRequestService.failResponseLog(operatorLogVo, duplicateTransactionException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_TRANSACTION_DUPLICATED.code);

        } catch (DuplicateRequestException duplicateRequestException) {
            operatorRequestService.failResponseLog(operatorLogVo, duplicateRequestException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_DUPLICATE_REQUEST.code);

        } catch (InvalidCurrencyException invalidCurrencyException) {
            operatorRequestService.failResponseLog(operatorLogVo, invalidCurrencyException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_WRONG_CURRENCY.code);

        } catch (ResponseNotMatchRequestException responseNotMatchRequestException) {
            operatorRequestService.failResponseLog(operatorLogVo, responseNotMatchRequestException.getClass().getName() +
                    " [" +responseNotMatchRequestException.getMessage()  + "]");
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        }
//        catch (Exception exception){
//            operatorRequestService.failResponseLog(operatorLogVo, exception.getClass().getName());
//            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
//        }

        return responseVo;

    }
}
