package com.nextgen.gameaggregator.vendor.aasexy.api.voidbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralRollbackDto;
import com.nextgen.gameaggregator.service.BetActionLogService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
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
public class VoidBetService {
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final BetActionLogService betActionLogService;

    @Autowired
    public VoidBetService(GameSessionService gameSessionService,
                          WalletService walletService,
                          HttpService httpService,
                          VendorService vendorService,
                          BetActionLogService betActionLogService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.betActionLogService = betActionLogService;
    }

    public VoidBetVo voidBet(HttpRequestLog httpRequestLog, HttpServletRequest request) {
        VoidBetVo vo = new VoidBetVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // Decode the URL-encoded message
            String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

            // Convert JsonNode back to JSON string
            String convertedJsonString = vendorService.convertBodyToJson(decodedBody);
            RequestDto<VoidBetDto> dto = HttpService.convertJsonToDto(convertedJsonString, new TypeReference<>() {
            });

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Process all transaction with completable future
            List<CompletableFuture<BigDecimal>> balanceList = new LinkedList<>();
            for (VoidBetTransactionsDto transaction : dto.getMessage().getTxns()) {
                CompletableFuture<BigDecimal> balance = CompletableFuture.supplyAsync(() -> processData(transaction, request));
                balanceList.add(balance);
            }

            // need waiting result for acceptance test, if not will get balance first
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

    private void doVerification(VoidBetTransactionsDto voidBetTransactionsDto, GameSession gameSession) throws
            InvalidPlayerException,
            GameNotSupportedException {

        // Verify valid game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(voidBetTransactionsDto.getGameCode()), GameNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), voidBetTransactionsDto.getUserId(), InvalidPlayerException::new);
    }

    private BigDecimal processData(VoidBetTransactionsDto voidBetTransactionsDto, HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestBody(new Gson().toJson(voidBetTransactionsDto));
        String traceId = httpRequestLog.getId();
        GameSession gameSession = null;
        BigDecimal balance = BigDecimal.ZERO;

        try {
            // Validate each user data
            this.doValidation(voidBetTransactionsDto);

            // Verify session token
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(voidBetTransactionsDto.getUserId());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(voidBetTransactionsDto.getGameCode(), gameSession);

            // Verify game session
            this.doVerification(voidBetTransactionsDto, gameSession);

            // Process Result
            balance = walletService.processRollback(traceId, voidBetTransactionsDto, gameSession, vendorService, httpRequestLog);

        } catch (InvalidRequestException | BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            //insert to collection
            this.prepareRollback(voidBetTransactionsDto, gameSession);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, new GeneralVo());
        }
        return balance;
    }

    private void prepareRollback(VoidBetTransactionsDto voidBetTransactionsDto, GameSession gameSession){
        THREAD_POOL.submit(() -> {
            GeneralRollbackDto generalRollbackDto = new ModelMapper().map(voidBetTransactionsDto, GeneralRollbackDto.class);
            betActionLogService.create(new Gson().toJson(generalRollbackDto), generalRollbackDto.getRoundId(), voidBetTransactionsDto.getRollbackId(),voidBetTransactionsDto.getRollbackId(),gameSession,1,null);
        });
    }
}
