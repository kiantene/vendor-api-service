package com.nextgen.gameaggregator.operator.wallet.bet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorLogVo;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.OperatorService;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class WalletBetAction {

    @Value("${testing.stub:false}")
    private Boolean useStub;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    OperatorService operatorService;

    public WalletBalanceVo call(String callbackUrl, String signature, WalletBetDto dto) throws InsufficientBalanceException, InvalidOperatorResponseException {

        // Call stub function instead if config file set to use stub
        if (useStub) {
            return operatorService.responseOperatorSub();
        }

        WalletBalanceVo responseVo = null;

        ResponseEntity apiResponse = WebClient.create(callbackUrl)
                .post()
                .uri(Endpoints.WALLET_BET)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(Endpoints.HEADER_SIGNATURE, signature)
                .body(BodyInserters.fromValue(dto))
                .retrieve()
                .onStatus(HttpStatus::isError, response -> Mono.empty())
                .toEntity(String.class)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        OperatorLogVo operatorLogVo = operatorService.createOperatorLogVo(Endpoints.WALLET_BET, callbackUrl, dto, apiResponse, signature, profilesActive);

        try {
            // 1. validate HTTP Response Code
            operatorService.validateOperatorHttpStatusResponse(apiResponse);

            //2. validate operator response
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), WalletBalanceVo.class);
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            operatorService.validateResponse(responseVo);

            //3. validate username and currency
            operatorService.validateResponseUserNameAndCurrency(responseVo, dto.getUsername(), dto.getCurrency());

            //TODO by Alex, to dicuss whether should we validate trace Id matching

            // 4. validate operator response fail status
            operatorService.operatorStatusException(responseVo.getStatus());

            BigDecimal balance = responseVo.getData().getBalance();
            //TODO to be discuss whether should system pre handle negative if
            boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
            if (isNegativeBalance) {
                throw new InsufficientBalanceException();
            }

            operatorService.successResponseLog(operatorLogVo);

            /**
             switch (responseVo.getStatus()) {
             case SC_OK -> {
             //To validate the username and currency is match with request
             if ((!responseVo.getData().getUsername().equals(dto.getUsername())) ||
             (!responseVo.getData().getCurrency().equals(dto.getCurrency()))) {
             throw new InvalidOperatorResponseException(responseVo.toString(), responseVo.getStatus().code);
             } else {
             operatorService.operatorResponseLogging(true, Endpoints.WALLET_BET, callbackUrl, dto, (String) apiResponse.getBody(), profilesActive);
             }
             BigDecimal balance = responseVo.getData().getBalance();
             //TODO to be discuss whether should system pre handle negative if
             boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
             if (isNegativeBalance) {
             operatorService.operatorResponseLogging(false, Endpoints.WALLET_BET, callbackUrl, dto, (String) apiResponse.getBody(), profilesActive);
             throw new InsufficientBalanceException(responseVo.toString());
             }
             }
             case SC_INSUFFICIENT_FUNDS -> {
             operatorService.operatorResponseLogging(false, Endpoints.WALLET_BET, callbackUrl, dto, (String) apiResponse.getBody(), profilesActive);
             throw new InsufficientBalanceException(responseVo.toString());
             }
             default -> {
             throw new InvalidOperatorResponseException(responseVo.toString(), responseVo.getStatus().code);
             }
             }
             **/

        } catch (HttpResponseStatusCodeException httpResponseStatusCodeException) {
            operatorService.failResponseLog(operatorLogVo, httpResponseStatusCodeException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (JsonSyntaxException jsonSyntaxException) {
            operatorService.failResponseLog(operatorLogVo, jsonSyntaxException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InvalidTokenException invalidTokenException) {
            operatorService.failResponseLog(operatorLogVo, invalidTokenException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_TOKEN.code);

        } catch (InvalidSignatureException invalidSignatureException) {
            operatorService.failResponseLog(operatorLogVo, invalidSignatureException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_SIGNATURE.code);

        } catch (InvalidPlayerException invalidPlayerException) {
            operatorService.failResponseLog(operatorLogVo, invalidPlayerException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_USER_NOT_EXISTS.code);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            operatorService.failResponseLog(operatorLogVo, disabledAgentPlayerException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (GameNotSupportedException gameNotSupportedException) {
            operatorService.failResponseLog(operatorLogVo, gameNotSupportedException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            operatorService.failResponseLog(operatorLogVo, insufficientBalanceException.getClass().getName());
            // throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code);
            //TODO by Alex, to check the response code
            throw new InsufficientBalanceException();

        } catch (InvalidRequestException invalidRequestException) {
            operatorService.failResponseLog(operatorLogVo, invalidRequestException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_REQUEST.code);

        } catch (BetNotFoundException betNotFoundException) {
            operatorService.failResponseLog(operatorLogVo, betNotFoundException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS.code);

        } catch (SystemMaintenanceException systemMaintenanceException) {
            operatorService.failResponseLog(operatorLogVo, systemMaintenanceException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNDER_MAINTENANCE.code);

        } catch (DuplicateTransactionException duplicateTransactionException) {
            operatorService.failResponseLog(operatorLogVo, duplicateTransactionException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_TRANSACTION_DUPLICATED.code);

        } catch (DuplicateRequestException duplicateRequestException) {
            operatorService.failResponseLog(operatorLogVo, duplicateRequestException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_DUPLICATE_REQUEST.code);

        } catch (InvalidCurrencyException invalidCurrencyException) {
            operatorService.failResponseLog(operatorLogVo, invalidCurrencyException.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_WRONG_CURRENCY.code);

        }catch (Exception exception){
            operatorService.failResponseLog(operatorLogVo, exception.getClass().getName());
            throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_UNKNOWN_ERROR.code);
        }

        return responseVo;
    }

}
