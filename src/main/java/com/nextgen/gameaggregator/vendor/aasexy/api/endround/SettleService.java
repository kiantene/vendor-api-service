package com.nextgen.gameaggregator.vendor.aasexy.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralSettleDto;
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
public class SettleService {
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final BetActionLogService betActionLogService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    @Autowired
    public SettleService(GameSessionService gameSessionService,
                         WalletService walletService,
                         HttpService httpService,
                         VendorService vendorService,
                         BetActionLogService betActionLogService,
                         RequestIdempotentLogService requestIdempotentLogService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.betActionLogService = betActionLogService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public SettleVo settle(HttpRequestLog httpRequestLog, HttpServletRequest request) {
        SettleVo vo = new SettleVo();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_SIZE);
        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // Decode the URL-encoded message
            String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

            // Convert JsonNode back to JSON string
            String convertedJsonString = vendorService.convertBodyToJson(decodedBody);
            RequestDto<SettleDto> dto = HttpService.convertJsonToDto(convertedJsonString, new TypeReference<>() {
            });

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Process all transaction with completable future
            // transaction list have different player id.
            List<CompletableFuture<BigDecimal>> balanceList = new LinkedList<>();
            for (SettleTransactionsDto transaction : dto.getMessage().getTxns()) {
                CompletableFuture<BigDecimal> balance = CompletableFuture.supplyAsync(() -> processData(transaction, request), executor);
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
        } finally {
            // close executor
            executor.shutdown();
        }
        return vo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(SettleTransactionsDto settleTransactionsDto, GameSession gameSession) throws InvalidPlayerException, GameNotSupportedException {
        // Verify valid game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(settleTransactionsDto.getGameId()), GameNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), settleTransactionsDto.getUserId(), InvalidPlayerException::new);
    }

    private BigDecimal processData(SettleTransactionsDto settleTransactionsDto, HttpServletRequest request){

        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestBody(new Gson().toJson(settleTransactionsDto));
        String traceId = httpRequestLog.getId();
        GameSession gameSession = null;
        ResultType resultType = null;
        BigDecimal balance = BigDecimal.ZERO;
        boolean isRequestExists = false;

        try {
            // Validate each user data
            this.doValidation(settleTransactionsDto);

            // Request idempotent checking for this transaction
            if (requestIdempotentLogService.checkExists(settleTransactionsDto, settleTransactionsDto.getUserId()) == null) {
                requestIdempotentLogService.create(settleTransactionsDto, settleTransactionsDto.getUserId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // Verify session token
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(settleTransactionsDto.getUserId());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(settleTransactionsDto.getGameCode(), gameSession);
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(settleTransactionsDto.getUserId());
                gameSessionService.updateByVendorGameCode(gameSession, settleTransactionsDto.getGameCode());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // Verify game session
            this.doVerification(settleTransactionsDto, gameSession);

            // Process Result
            resultType = vendorService.calculateResultType(settleTransactionsDto.getBetAmount(), settleTransactionsDto.getWinAmount(), settleTransactionsDto.getJackpotAmount(), settleTransactionsDto.getSettleType());
            balance = walletService.processBetResult(traceId, gameSession, settleTransactionsDto, resultType, vendorService, httpRequestLog);

        } catch (InvalidRequestException | TransactionStillProcessingException
                | BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
        } catch (Exception e){
            // if failed then insert to collection
            this.prepareSettleBet(settleTransactionsDto, gameSession, resultType);
            httpService.logError(httpRequestLog, e);
        } finally {
            if (!isRequestExists) {
                // first request (not request exist) will delete log after process finish.
                requestIdempotentLogService.delete(settleTransactionsDto, settleTransactionsDto.getUserId());
            }
            httpService.end(httpRequestLog, new GeneralVo());
        }
        return balance;
    }

    private void prepareSettleBet(SettleTransactionsDto settleTransactionsDto, GameSession gameSession,ResultType resultType){
        THREAD_POOL.submit(() -> {
            GeneralSettleDto generalSettleDto = new ModelMapper().map(settleTransactionsDto, GeneralSettleDto.class);
            betActionLogService.create(new Gson().toJson(generalSettleDto), generalSettleDto.getRoundId(), generalSettleDto.getVendorBetId(),generalSettleDto.getExternalTransactionId(),gameSession,2,resultType);
        });
    }
}
