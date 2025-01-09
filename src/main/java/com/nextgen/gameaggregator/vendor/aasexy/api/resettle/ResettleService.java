package com.nextgen.gameaggregator.vendor.aasexy.api.resettle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralAdjustmentDto;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aasexy.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aasexy.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import com.nextgen.gameaggregator.vendor.aasexy.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class ResettleService {
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final WalletAdjustmentService walletAdjustmentService;
    private final SettledBetService settledBetService;
    private final BetActionLogService betActionLogService;

    @Autowired
    public ResettleService(GameSessionService gameSessionService, VendorLineService vendorLineService, WalletService walletService, HttpService httpService, VendorService vendorService, WalletAdjustmentService walletAdjustmentService, ValidationService validationService, SettledBetService settledBetService, BetActionLogService betActionLogService) {
        this.gameSessionService = gameSessionService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.walletAdjustmentService = walletAdjustmentService;
        this.settledBetService = settledBetService;
        this.betActionLogService = betActionLogService;
    }

    public ResettleVo resettle(HttpRequestLog httpRequestLog, HttpServletRequest request) {
        ResettleVo vo = new ResettleVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // Decode the URL-encoded message
            String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

            // Convert JsonNode back to JSON string
            String convertedJsonString = vendorService.convertBodyToJson(decodedBody);
            RequestDto<ResettleDto> dto = HttpService.convertJsonToDto(convertedJsonString, new TypeReference<>() {
            });

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Process all transaction with completable future
            // Transaction list have different player id.
            List<CompletableFuture<BigDecimal>> balanceList = new LinkedList<>();
            for (ResettleTransactionsDto transaction : dto.getMessage().getTxns()) {
                CompletableFuture<BigDecimal> balance = CompletableFuture.supplyAsync(() -> processData(transaction, request));
                balanceList.add(balance);
            }

            // need waiting result for acceptance test, vendor need get latest balance
            vendorService.processMultipleDataResponse(balanceList);

        } catch (JsonProcessingException | InvalidRequestException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_PARAMETERS);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            vo.setResponseCodes(ResponseCodes.FAIL);
            httpService.logError(httpRequestLog, e);
        }
        return vo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(ResettleTransactionsDto resettleTransactionsDto, GameSession gameSession) throws
            InvalidPlayerException,
            GameNotSupportedException {

        // Verify valid game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(resettleTransactionsDto.getGameId()), GameNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), resettleTransactionsDto.getUserId(), InvalidPlayerException::new);
    }

    private BigDecimal processData(ResettleTransactionsDto resettleTransactionsDto, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestBody(new Gson().toJson(resettleTransactionsDto));
        String traceId = httpRequestLog.getId();
        GameSession gameSession = null;
        BigDecimal balance = BigDecimal.ZERO;

        try {
            // Validate each user data
            this.doValidation(resettleTransactionsDto);

            // Verify session token
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(resettleTransactionsDto.getUserId());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(resettleTransactionsDto.getGameCode(), gameSession);
            }catch (AuthenticationException e){
                gameSession = gameSessionService.generateNewSessionToken(resettleTransactionsDto.getUserId());
                gameSessionService.updateByVendorGameCode(gameSession, resettleTransactionsDto.getGameCode());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // Verify game session
            this.doVerification(resettleTransactionsDto, gameSession);

            // Get settle bet to calculate adjustment amount
            SettledBet settledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(resettleTransactionsDto.getVendorBetId(), resettleTransactionsDto.getRoundId(), gameSession.getVendorId(), gameSession.getVendorPlayerId());
            resettleTransactionsDto.setAdjustmentAmount(resettleTransactionsDto.getWinAmount().subtract(settledBet.getWinAmount()));

            // Process Result
            balance = walletAdjustmentService.processAdjustment(traceId, gameSession, resettleTransactionsDto, httpRequestLog);

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            // insert to collection
            this.prepareAdjustmentBet(resettleTransactionsDto,gameSession);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, new GeneralVo());
        }
        return balance;
    }

    private void prepareAdjustmentBet(ResettleTransactionsDto resettleTransactionsDto, GameSession gameSession){
        THREAD_POOL.submit(() -> {
            GeneralAdjustmentDto generalAdjustmentDto = new ModelMapper().map(resettleTransactionsDto, GeneralAdjustmentDto.class);
            betActionLogService.create(new Gson().toJson(generalAdjustmentDto), generalAdjustmentDto.getRoundId(), generalAdjustmentDto.getVendorBetId(),generalAdjustmentDto.getExternalTransactionId(),gameSession,3,null);
        });
    }
}
