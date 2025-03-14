package com.nextgen.gameaggregator.vendor.aasexy.api.canceltips;

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
public class CancelTipsService {
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final BetActionLogService betActionLogService;

    @Autowired
    public CancelTipsService(GameSessionService gameSessionService,
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

    public CancelTipsVo cancelTips(HttpRequestLog httpRequestLog, HttpServletRequest request, String traceId) {
        CancelTipsVo vo = new CancelTipsVo();
        GameSession gameSession = null;

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // Decode the URL-encoded message
            String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

            // Convert JsonNode back to JSON string
            String convertedJsonString = vendorService.convertBodyToJson(decodedBody);
            RequestDto<CancelTipsDto> dto = HttpService.convertJsonToDto(convertedJsonString, new TypeReference<>() {
            });

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getMessage().getTxns().get(0).getUserId());
            }catch (AuthenticationException e){
                gameSession = gameSessionService.generateNewSessionToken(dto.getMessage().getTxns().get(0).getUserId());
                gameSessionService.updateByVendorGameCode(gameSession, dto.getMessage().getTxns().get(0).getGameCode());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            List<CompletableFuture<BalanceVo>> balanceVoList = new LinkedList<>();
            for (CancelTipsTransactionsDto transaction : dto.getMessage().getTxns()) {
                GameSession finalGameSession = gameSession;
                CompletableFuture<BalanceVo> balance = CompletableFuture.supplyAsync(() -> processData(transaction, request, finalGameSession));
                balanceVoList.add(balance);
            }

            // cannot get last balance
            BigDecimal balance = vendorService.checkResponseAndReturnBalance(balanceVoList);
            if(balance == null){
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

    private void doValidation(CancelTipsTransactionsDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doValidation(RequestDto<CancelTipsDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        if(StringUtils.isBlank(dto.getMessage().getTxns().get(0).getGameCode())){
            throw new InvalidRequestException();
        }
        if(StringUtils.isBlank(dto.getMessage().getTxns().get(0).getUserId())){
            throw new InvalidRequestException();
        }
    }

    private void doVerification(CancelTipsTransactionsDto cancelTipsTransactionsDto, GameSession gameSession) throws
            InvalidPlayerException,
            GameNotSupportedException {

        // Verify valid game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(cancelTipsTransactionsDto.getGameCode()), GameNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), cancelTipsTransactionsDto.getUserId(), InvalidPlayerException::new);
    }

    private BalanceVo processData(CancelTipsTransactionsDto cancelTipsTransactionsDto, HttpServletRequest request, GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestBody(new Gson().toJson(cancelTipsTransactionsDto));
        String traceId = httpRequestLog.getId();
        BalanceVo balanceVo = null;

        try {
            // 1. Validate each user data
            this.doValidation(cancelTipsTransactionsDto);

            // 2. Verify session token
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(cancelTipsTransactionsDto.getGameCode(), gameSession);

            this.doVerification(cancelTipsTransactionsDto, gameSession);

            BigDecimal balance = walletService.processRollback(traceId, cancelTipsTransactionsDto, gameSession, vendorService, httpRequestLog);
            balanceVo = new BalanceVo(balance, httpRequestLog.getOperatorTimestamp());

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e){
            balanceVo = new BalanceVo(e.getBalance(), httpRequestLog.getOperatorTimestamp());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            //do nothing
            this.prepareRollback(cancelTipsTransactionsDto, gameSession);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, new GeneralVo());
        }
        return balanceVo;
    }
    private void prepareRollback(CancelTipsTransactionsDto cancelTipsTransactionsDto, GameSession gameSession){
        THREAD_POOL.submit(() -> {
            GeneralRollbackDto generalRollbackDto = new ModelMapper().map(cancelTipsTransactionsDto, GeneralRollbackDto.class);
            betActionLogService.create(new Gson().toJson(generalRollbackDto), cancelTipsTransactionsDto.getRoundId(), cancelTipsTransactionsDto.getRollbackId(),cancelTipsTransactionsDto.getRollbackId(),gameSession,1,null);
        });
    }
}
