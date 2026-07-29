package com.nextgen.gameaggregator.vendor.mg.api.betresult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextHolder;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.data.kafka.betdetails.BetDetailEmitRequest;
import com.nextgen.gameaggregator.data.kafka.betdetails.EventKind;
import com.nextgen.gameaggregator.data.kafka.betdetails.RawBetDetailsProducer;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.mg.api.promo.PromoPayoutHandler;
import com.nextgen.gameaggregator.vendor.mg.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.mg.constant.Headers;
import com.nextgen.gameaggregator.vendor.mg.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.mg.constant.TxnType.CREDIT;
import static com.nextgen.gameaggregator.vendor.mg.constant.TxnType.DEBIT;

@RestController
@RequestMapping(path = Endpoints.PATH)
@Slf4j
public class UpdateBalanceAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;
    @Autowired
    private PromoPayoutHandler promoPayoutHandler;
    @Autowired
    private RawBetDetailsProducer rawBetDetailsProducer;

    @PostMapping(path = Endpoints.UPDATE_BALANCE)
    public ResponseEntity<UpdateBalanceVo> updateBalance(HttpServletRequest request) {
        // Autowire the VendorService bean
        VendorService vendorService = new VendorService();
        autowireCapableBeanFactory.autowireBean(vendorService);
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Get start time of request
        long startTime = System.currentTimeMillis();
        // Get the trace ID from the logging
        String traceId = httpRequestLog.getId();
        HttpStatus status = HttpStatus.OK;
        UpdateBalanceVo updateBalanceVo = new UpdateBalanceVo();
        HttpHeaders headers = new HttpHeaders();

        GameSession gameSession = null;
        // Determine message for checkUnsettleAndSettleBet
        StringBuilder message = new StringBuilder();
        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert the request body to a UpdateBalanceDto object
            UpdateBalanceDto dto = HttpService.convertJsonToDto(body, UpdateBalanceDto.class);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            if (dto.getIsFreespin() == 1 && dto.getMetaData().getFreeGame() != null &&
                    dto.getMetaData().getFreeGame().getOfferGuid() != null && dto.getTxnType() == CREDIT) {
                updateBalanceVo = promoPayoutHandler.promo(dto).getBody();
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                headers.add(Headers.RESPONSE_TIMESTAMP, String.valueOf(responseTime));
                // Add back the requestId to the response headers
                headers.add(Headers.REQUEST_ID, request.getHeader(Headers.REQUEST_ID));
                // Return ResponseEntity with UpdateBalanceDto object, headers, and HTTP status code
                return new ResponseEntity<>(updateBalanceVo, headers, status);
            }

            try {
                gameSession = gameSessionService.verifyToken(dto.getExtOperatorToken());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getContentCode(), gameSession);
            } catch (Exception e) {
                if (dto.getTxnType() == DEBIT) {
                    //DEBIT request token will not be regenerated.
                    throw new AuthenticationException(e.getMessage());
                } else {
                    gameSession = gameSessionService.generateNewSessionToken(dto.getPlayerId());
                    gameSessionService.updateByVendorGameCode(gameSession, dto.getGameId());
                    gameSessionService.updateByVendorCurrencyId(gameSession);
                    gameSession.setToken(traceId);
                    gameSession.setVendorToken(traceId);
                }
            }

            switch (dto.getTxnType()) {
                case DEBIT -> {
                    validationService.validateEligibleBet(gameSession, dto.getPlayerId());
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);
                    BigDecimal balance = betEvent.getLastBalance();
                    updateBalanceVo.setCurrency(gameSession.getVendorCurrencyCode());
                    updateBalanceVo.setBalance(balance);

                    this.emitRawBetDetail(gameSession, dto.getVendorBetId(), dto.getRoundId(), EventKind.PLACE_BET, betEvent.getBetInformation().getBetId(), body);
                }
                case CREDIT -> {
                    WinDataDto winDataDto = new ObjectMapper().convertValue(dto, WinDataDto.class);
                    //this.checkUnsettleAndSettleBet(winDataDto, gameSession, message, vendorService);
                    ResultType resultType = preProcessWinDto(winDataDto, gameSession);
                    // if completed true then send round ended info
                    if (winDataDto.getCompleted()) {
                        BetResultContextHolder.initialise()
                                .configure(config -> config.setSettleType(SettleType.ROUND));
                        BetResultContext betResultContext = BetResultContextHolder.getBetResultContext();
                        betResultContext.setRoundEnded(BetStatus.SETTLED.isValueOf(winDataDto.getBetStatus().code));
                    }
                    BigDecimal balance = walletService.processBetResult(traceId, gameSession, winDataDto, resultType, vendorService, httpRequestLog);
                    updateBalanceVo.setCurrency(gameSession.getVendorCurrencyCode());
                    updateBalanceVo.setBalance(balance);

                    this.emitRawBetDetail(gameSession, winDataDto.getVendorBetId(), winDataDto.getRoundId(), EventKind.RESULT_UPDATE, httpRequestLog.getGaBetId(), body);
                }
                default -> status = HttpStatus.INTERNAL_SERVER_ERROR;
            }

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            updateBalanceVo.setCurrency(gameSession.getVendorCurrencyCode());
            updateBalanceVo.setBalance(betResultIdempotentViolationException.getBalance());

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            // Vendor only accept status 200, 400, 402, 404, 500
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            status = HttpStatus.BAD_REQUEST;

        } catch (JsonProcessingException jsonProcessingException) {
            httpService.logError(httpRequestLog, jsonProcessingException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (InvalidRequestException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (BetNotFoundException betNotFoundException) {
            httpService.logError(httpRequestLog, new BetNotFoundException(betNotFoundException.getMessage() + " | " + message.toString()));
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (InvalidPlayerException invalidPlayerException) {
            httpService.logError(httpRequestLog, invalidPlayerException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (DisabledVendorLineException disabledVendorLineException) {
            httpService.logError(httpRequestLog, disabledVendorLineException);
            status = HttpStatus.BAD_REQUEST;

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            httpService.logError(httpRequestLog, disabledAgentPlayerException);
            status = HttpStatus.BAD_REQUEST;

        } catch (DisabledGameException disabledGameException) {
            httpService.logError(httpRequestLog, disabledGameException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            status = HttpStatus.PAYMENT_REQUIRED;

        } catch (TransactionStillProcessingException internalErrorException) {
            httpService.logError(httpRequestLog, internalErrorException);
            status = HttpStatus.BAD_REQUEST;

        } catch (Exception exception) { // any other exception encountered
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, updateBalanceVo);
        }

        // Calculate response time and add it to the headers
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        headers.add(Headers.RESPONSE_TIMESTAMP, String.valueOf(responseTime));
        // Add back the requestId to the response headers
        headers.add(Headers.REQUEST_ID, request.getHeader(Headers.REQUEST_ID));
        // Return ResponseEntity with UpdateBalanceDto object, headers, and HTTP status code
        return new ResponseEntity<>(updateBalanceVo, headers, status);
    }

    private void emitRawBetDetail(GameSession gameSession, String vendorBetId, String roundId, EventKind eventKind, String gaBetId, String body) {
        if (gameSession == null) {
            log.warn("Skipping {} raw bet detail emit: gameSession is null eventKind={} vendorBetId={} roundId={}",
                    Endpoints.VENDOR, eventKind, vendorBetId, roundId);
            return;
        }
        try {
            rawBetDetailsProducer.emit(BetDetailEmitRequest.builder()
                    .vendor(Endpoints.VENDOR)
                    .eventKind(eventKind)
                    .vendorBetId(vendorBetId)
                    .gaBetId(gaBetId)
                    .roundId(roundId)
                    .vendorPlayerUsername(gameSession.getVendorPlayerUsername())
                    .agentId(gameSession.getAgentId())
                    .gameCategoryId(gameSession.getGameCategoryId())
                    .bodyFormat(Endpoints.BODY_FORMAT)
                    .requestBody(body)
                    .build());
        } catch (Exception e) {
            log.warn("{} raw bet detail emit failed eventKind={} vendorBetId={} roundId={}: {}",
                    Endpoints.VENDOR, eventKind, vendorBetId, roundId, e.getMessage());
        }
    }

    private void doValidation(UpdateBalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private ResultType preProcessWinDto(WinDataDto dto, GameSession gameSession) {
        //only for live casino calculation
        if (gameSession.getGameCategoryId() == 5) {
            ResultType resultType = dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : dto.getCompleted() ? ResultType.END : ResultType.BET_LOSE;

            if (resultType == ResultType.BET_WIN || resultType == ResultType.BET_LOSE) {
                dto.setWinLoss(dto.getWinAmount());
            }
            return resultType;
        }
        // Completed True also will happen in Win Situation
        return dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : dto.getCompleted() ? ResultType.END : ResultType.LOSE;
    }
}
