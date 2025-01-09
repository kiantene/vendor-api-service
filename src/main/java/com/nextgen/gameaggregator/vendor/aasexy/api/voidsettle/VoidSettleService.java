package com.nextgen.gameaggregator.vendor.aasexy.api.voidsettle;

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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class VoidSettleService {
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final BetActionLogService betActionLogService;

    @Autowired
    public VoidSettleService(GameSessionService gameSessionService, WalletService walletService, HttpService httpService, VendorService vendorService, BetActionLogService betActionLogService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.betActionLogService = betActionLogService;
    }

    public VoidSettleVo voidSettle(HttpRequestLog httpRequestLog, HttpServletRequest request) {
        VoidSettleVo vo = new VoidSettleVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // Decode the URL-encoded message
            String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

            // Convert JsonNode back to JSON string
            String convertedJsonString = vendorService.convertBodyToJson(decodedBody);
            RequestDto<VoidSettleDto> dto = HttpService.convertJsonToDto(convertedJsonString, new TypeReference<>() {
            });

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Process all transaction with completable future
            for (VoidSettleTransactionsDto transaction : dto.getMessage().getTxns()) {
                CompletableFuture.runAsync(() -> processData(transaction, request));
            }

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

    private void doVerification(VoidSettleTransactionsDto voidSettleTransactionsDto, GameSession gameSession) throws
            InvalidPlayerException,
            GameNotSupportedException {

        // Verify valid game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(voidSettleTransactionsDto.getGameCode()), GameNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), voidSettleTransactionsDto.getUserId(), InvalidPlayerException::new);
    }

    private void processData(VoidSettleTransactionsDto voidSettleTransactionsDto, HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestBody(new Gson().toJson(voidSettleTransactionsDto));
        String traceId = httpRequestLog.getId();
        GameSession gameSession = null;

        try {
            // Validate each user data
            this.doValidation(voidSettleTransactionsDto);

            // Verify session token
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(voidSettleTransactionsDto.getUserId(), voidSettleTransactionsDto.getGameCode());

            // Verify game session
            this.doVerification(voidSettleTransactionsDto, gameSession);

            // Process Result
            walletService.processRollback(traceId, voidSettleTransactionsDto, gameSession, vendorService, httpRequestLog);

        } catch (InvalidRequestException | BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            //insert to collection
            this.prepareRollback(voidSettleTransactionsDto, gameSession);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, new GeneralVo());
        }
    }
    private void prepareRollback(VoidSettleTransactionsDto voidSettleTransactionsDto, GameSession gameSession){
        THREAD_POOL.submit(() -> {
            GeneralRollbackDto generalRollbackDto = new ModelMapper().map(voidSettleTransactionsDto, GeneralRollbackDto.class);
            betActionLogService.create(new Gson().toJson(generalRollbackDto), generalRollbackDto.getRoundId(), voidSettleTransactionsDto.getRollbackId(),voidSettleTransactionsDto.getRollbackId(),gameSession,1,null);
        });
    }
}
