package com.nextgen.gameaggregator.operator.sport.resettle;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameRepository;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
public class SportResettleAction {
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AgentPlayerRepository agentPlayerRepository;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private CurrencyConversionService currencyConversionService;
    @Autowired
    private RequestService requestService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorGameRepository vendorGameRepository;
    @Autowired
    private BetResultRetryLogService betResultRetryLogService;

    public WalletBalanceVo call(String traceId, SportSettledBet sportSettledBet, SportResettleData sportResettleData, HttpRequestLog httpRequestLog, VendorCurrency vendorCurrency, AgentPlayer agentPlayer) throws VendorCurrencyNotSupportException, InvalidAgentApiCredentialException, InvalidOperatorResponseException {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        WalletBalanceVo responseVo = null;
        ResponseCodes.Status defaultResponses = ResponseCodes.Status.SC_OK;
        Integer agentId = sportSettledBet.getAgentId();

        AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
        String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

        String gameCode = vendorGameRepository.findById(sportSettledBet.getVendorGameId()).map(VendorGame::getCode).orElse(null);

        SportResettleDto dto = this.newSportResettleDto(traceId, agentPlayer.getUsername(), sportSettledBet, sportResettleData, gameCode, vendorCurrency);

        String signature = authenticationService.generateSignature(dto, agentApiCredential.getApiSecret());
        headerMap.add(EndPoints.HEADER_SIGNATURE, signature);
        headerMap.add(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey());

        long startTime = System.currentTimeMillis();
        if (httpRequestLog != null) {
            httpRequestLog.setAgentId(agentId);
            httpRequestLog.setOperatorStart(startTime);

            String jsonApiResponse = new Gson().toJson(dto);
            httpRequestLog.setOperatorData(jsonApiResponse);
            httpRequestLog.setOperatorEndPoints(apiUrl + EndPoints.SPORT_RESETTLE);
        }

        try {
            ResponseEntity<String> apiResponse = WebClient.create(apiUrl).post().uri(EndPoints.SPORT_RESETTLE)
                    .header(EndPoints.HEADER_SIGNATURE, signature)
                    .header(EndPoints.HEADER_API_KEY, agentApiCredential.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(EndPoints.SPORTBOOK_TIMEOUT))
                    .block();

            long endTime = System.currentTimeMillis();

            if (httpRequestLog != null) {
                if (apiResponse != null) {
                    httpRequestLog.setOperatorHttpStatusCode(apiResponse.getStatusCode().value());
                }
                httpRequestLog.setOperatorEnd(endTime);
            }

            requestService.createRequestLogVo(EndPoints.SPORT_RESETTLE, apiUrl, dto, apiResponse, headerMap, startTime, endTime, this.getClass().getPackage().getName(), profilesActive);

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate operator response
            if (apiResponse == null) throw new InvalidOperatorResponseException();
            responseVo = new Gson().fromJson(apiResponse.getBody(), WalletBalanceVo.class);

            if (httpRequestLog != null) {
                httpRequestLog.setOperatorResponse(apiResponse.getBody());
                httpRequestLog.setOperatorResponseStatus(responseVo.getStatus());
            }

            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidOperatorResponseException(ResponseCodes.Status.SC_INVALID_RESPONSE.code));
            RequestService.validateResponse(responseVo);

            //3. validate username and currency
            requestService.validateResponseMatchRequest(responseVo, dto.getUsername(), dto.getCurrency(), dto.getTraceId());

            // 4. validate operator response fail status
            requestService.operatorStatusException(responseVo.getStatus());

            // 5. add conversion rate when returning the balance to vendor
            currencyConversionService.doCurrencyConversionRateToVendor(responseVo, vendorCurrency.getToVendorRate());

            BigDecimal balance = responseVo.getData().getBalance();

            boolean isNegativeBalance = balance.compareTo(BigDecimal.ZERO) < 0;
            if (isNegativeBalance) {
                throw new InvalidOperatorResponseException(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code);
            }

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException |
                 ResponseNotMatchRequestException invalidResponseException) {
            defaultResponses = ResponseCodes.Status.SC_INVALID_RESPONSE;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //Integer operatorStatus = invalidOperatorResponseException.getOperatorStatus();
            //TODO TEMPORARY RESPONSES INVALID RESPONSES
            defaultResponses = ResponseCodes.Status.SC_INVALID_RESPONSE;

        } catch (Exception exception) {
            if (exception.getMessage().contains("java.util.concurrent.TimeoutException")) {
                httpRequestLog.setErrorMessage(exception.getMessage());
            }
            defaultResponses = ResponseCodes.Status.SC_UNKNOWN_ERROR;

        } finally {
            if (defaultResponses == ResponseCodes.Status.SC_OK) {
                //do nothing if success

            } else {
                responseVo = betResultRetryLogService.processForceSuccess(traceId, agentPlayer.getUsername(), vendorCurrency.getCurrency().getCode(), sportSettledBet);
                betResultRetryLogService.create(httpRequestLog.getOperatorData(), vendorCurrency.getVendorId(), agentPlayer.getAgentId(), sportSettledBet.getBetId(), sportSettledBet.getRoundId(), sportSettledBet.getInternalTransactionId(), EndPoints.SPORT_RESETTLE);
            }

        }

        return responseVo;
    }

    private SportResettleDto newSportResettleDto(String traceId, String agentPlayerUsername, SportSettledBet sportSettledBet, SportResettleData sportResettleData, String gameCode, VendorCurrency vendorCurrency) {
        BigDecimal betAmount = new BigDecimal(Optional.ofNullable(sportSettledBet.getNewBetAmount()).orElse(sportSettledBet.getBetAmount()).stripTrailingZeros().toPlainString());
        BigDecimal winAmount = new BigDecimal(sportSettledBet.getWinAmount().stripTrailingZeros().toPlainString());
        BigDecimal winLoss = new BigDecimal(sportSettledBet.getWinLoss().stripTrailingZeros().toPlainString());
        BigDecimal newWinAmount = new BigDecimal(sportResettleData.getNewWinAmount().stripTrailingZeros().toPlainString());
        BigDecimal creditAmount = BigDecimal.ZERO;
        BigDecimal debitAmount = BigDecimal.ZERO;

        if (newWinAmount.compareTo(winAmount) > 0) {
            creditAmount = newWinAmount.subtract(winAmount);

        } else if (newWinAmount.compareTo(winAmount) < 0) {
            debitAmount = winAmount.subtract(newWinAmount);
        }

        SportResettleDto sportResettleDto = new SportResettleDto();
        sportResettleDto.setTraceId(traceId);
        sportResettleDto.setUsername(agentPlayerUsername);
        sportResettleDto.setTransactionId(sportSettledBet.getInternalTransactionId());
        sportResettleDto.setExternalTransactionId(sportSettledBet.getExternalTransactionId());
        sportResettleDto.setBetId(sportSettledBet.getBetId());
        sportResettleDto.setRoundId(sportSettledBet.getRoundId());
        sportResettleDto.setBetAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(betAmount, vendorCurrency.getFromVendorRate()));
        sportResettleDto.setWinAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(winAmount, vendorCurrency.getFromVendorRate()));
        sportResettleDto.setNewWinAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(newWinAmount, vendorCurrency.getFromVendorRate()));
        sportResettleDto.setWinLoss(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(winLoss, vendorCurrency.getFromVendorRate()));
        sportResettleDto.setDebitAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(debitAmount, vendorCurrency.getFromVendorRate()));
        sportResettleDto.setCreditAmount(currencyConversionService.doCurrencyConversionRateFromVendorForAmount(creditAmount, vendorCurrency.getFromVendorRate()));
        sportResettleDto.setGameCode(gameCode);
        sportResettleDto.setCurrency(vendorCurrency.getCurrency().getCode());
        sportResettleDto.setTimestamp(sportSettledBet.getVendorBetTime());

        return sportResettleDto;
    }
}
