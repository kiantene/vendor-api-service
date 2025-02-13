package com.nextgen.gameaggregator.vendor.aasexy.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralRollbackDto;
import com.nextgen.gameaggregator.service.BetActionLogService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aasexy.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aasexy.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import com.nextgen.gameaggregator.vendor.aasexy.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.aasexy.vo.GeneralVo;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class CancelService {
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final BetActionLogService betActionLogService;

    @Autowired
    public CancelService(GameSessionService gameSessionService,
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

    public CancelVo cancel(HttpRequestLog httpRequestLog, HttpServletRequest request, String traceId) {
        CancelVo vo = new CancelVo();
        RequestDto<CancelDto> dto = null;
        GameSession gameSession = null;

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // Decode the URL-encoded message
            String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

            // Convert JsonNode back to JSON string
            String convertedJsonString = vendorService.convertBodyToJson(decodedBody);
            dto = HttpService.convertJsonToDto(convertedJsonString, new TypeReference<>() {
            });

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get first transaction user id, game code for regenerate token
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getMessage().getTxns().get(0).getUserId());
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(dto.getMessage().getTxns().get(0).getUserId());
                gameSessionService.updateByVendorGameCode(gameSession, dto.getMessage().getTxns().get(0).getGameCode());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // Process all transaction with completable future
            List<CompletableFuture<BalanceVo>> balanceVoList = new LinkedList<>();
            for (CancelTransactionsDto transaction : dto.getMessage().getTxns()) {
                GameSession finalGameSession = gameSession;
                CompletableFuture<BalanceVo> balanceVo = CompletableFuture.supplyAsync(() -> processData(transaction, request, finalGameSession));
                balanceVoList.add(balanceVo);
            }

            // Get latest balance, if have fail response then return error
            BigDecimal balance = vendorService.checkResponseAndReturnBalance(balanceVoList);
            if (balance == null) {
                // will return success, so need get current balance
                balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
            }

            vo.setBalance(balance.setScale(3, RoundingMode.DOWN));
            vo.setBalanceTs(vendorService.convertDateTimeFormat(System.currentTimeMillis()));

        } catch (InvalidPlayerException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_USER_ID);
            httpService.logError(httpRequestLog, e);
        } catch (GameNotSupportedException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_GAME);
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException | InvalidRequestException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_PARAMETERS);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            vo.setResponseCodes(ResponseCodes.FAIL);
            httpService.logError(httpRequestLog, e);
        }
        return vo;
    }

    private void doValidation(CancelTransactionsDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doValidation(RequestDto<CancelDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Check first transaction game code
        if(StringUtils.isBlank(dto.getMessage().getTxns().get(0).getGameCode())){
            throw new InvalidRequestException();
        }
        // Check first transaction user id
        if(StringUtils.isBlank(dto.getMessage().getTxns().get(0).getUserId())){
            throw new InvalidRequestException();
        }
    }

    private void doVerification(CancelTransactionsDto cancelTransactionsDto, GameSession gameSession) throws
            InvalidPlayerException,
            GameNotSupportedException {

        // Verify valid game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(cancelTransactionsDto.getGameCode()), GameNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), cancelTransactionsDto.getUserId(), InvalidPlayerException::new);
    }

    private BalanceVo processData(CancelTransactionsDto cancelTransactionsDto, HttpServletRequest request, GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestBody(new Gson().toJson(cancelTransactionsDto));
        String traceId = httpRequestLog.getId();
        BalanceVo balanceVo = null;

        try {
            // Validate each user data
            this.doValidation(cancelTransactionsDto);

            // Verify session token
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(cancelTransactionsDto.getGameCode(), gameSession);

            // Verify game session
            this.doVerification(cancelTransactionsDto, gameSession);

            // Process Result
            BigDecimal balance = walletService.processRollback(traceId, cancelTransactionsDto, gameSession, vendorService, httpRequestLog);
            balanceVo = new BalanceVo(balance, httpRequestLog.getOperatorTimestamp());

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e){
            balanceVo = new BalanceVo(e.getBalance(), httpRequestLog.getOperatorTimestamp());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            // failed then insert to collection for process rollback
            this.prepareRollback(cancelTransactionsDto, gameSession);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, new GeneralVo());
        }
        return balanceVo;
    }

    private void prepareRollback(CancelTransactionsDto cancelTransactionsDto, GameSession gameSession){
        THREAD_POOL.submit(() -> {
            GeneralRollbackDto generalRollbackDto = new ModelMapper().map(cancelTransactionsDto, GeneralRollbackDto.class);
            betActionLogService.create(new Gson().toJson(generalRollbackDto), cancelTransactionsDto.getRoundId(), cancelTransactionsDto.getRollbackId(),cancelTransactionsDto.getRollbackId(),gameSession,1,null);
        });
    }
}
