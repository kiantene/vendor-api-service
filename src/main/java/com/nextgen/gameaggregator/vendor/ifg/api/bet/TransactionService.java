package com.nextgen.gameaggregator.vendor.ifg.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ifg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ifg.constant.RoundBetStatus;
import com.nextgen.gameaggregator.vendor.ifg.service.VendorService;
import com.nextgen.gameaggregator.vendor.ifg.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.ErrorVo;
import org.springframework.stereotype.Service;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public TransactionService(GameSessionService gameSessionService,
                              VendorLineService vendorLineService,
                              WalletService walletService,
                              HttpService httpService,
                              AgentPlayerService agentPlayerService,
                              VendorGameService vendorGameService,
                              VendorService vendorService,
                              RequestIdempotentLogService requestIdempotentLogService) {
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo transaction(HttpRequestLog httpRequestLog, String traceId) {
        TransactionServiceDto transactionServiceDto = new TransactionServiceDto();
        TransactionServiceVo vo = new TransactionServiceVo();
        BalanceVo balanceVo = new BalanceVo();
        RoundBetVo roundBetVo = new RoundBetVo();
        ErrorVo errorVo = new ErrorVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();
        BigDecimal balance;
        boolean isRequestExists = false;
        ResultType resultType;
        try {
            transactionServiceDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), TransactionServiceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(transactionServiceDto);

            // Request idempotent checking.
            if (requestIdempotentLogService.checkExists(transactionServiceDto, transactionServiceDto.getRoundbet().getWlid()) == null) {
                requestIdempotentLogService.create(transactionServiceDto, transactionServiceDto.getRoundbet().getWlid());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // Verify session token
            gameSession = gameSessionService.verifyToken(transactionServiceDto.getRoundbet().getGuid());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(transactionServiceDto, gameSession);

            if (RoundBetStatus.FINISHED.code.equals(transactionServiceDto.getRoundbet().getFinished())) {
                resultType = vendorService.calculateResultType(transactionServiceDto.getBetAmount(), transactionServiceDto.getWinAmount(),
                        transactionServiceDto.getJackpotAmount(), true);
                balance = walletService.processBetResult(traceId, gameSession, transactionServiceDto, resultType, vendorService, httpRequestLog);
            } else {
                // Process Bet
                BetEvent betEvent = walletService.processBet(traceId, gameSession, transactionServiceDto, httpRequestLog.getRequestBody(), httpRequestLog);
                balance = betEvent.getLastBalance();
            }
            // set balanceVo
            balanceVo.setValue(String.valueOf(balance.toBigInteger()));
            balanceVo.setVersion(String.valueOf(System.currentTimeMillis()));
            balanceVo.setType("real");
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());

            // set roundBetVo
            roundBetVo.setBalance(balanceVo);
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.RESULT_SUCCESS);

            // set vo
            vo.setRoundbet(roundBetVo);
        } catch (InsufficientBalanceException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.NOT_ENOUGH_MONEY);
            errorVo.setMsg(ResponseCodes.N_E_M);

            // set roundBetVo
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.RESULT_FAIL);
            roundBetVo.setError(errorVo);

            // set vo
            vo.setRoundbet(roundBetVo);

            httpService.logError(httpRequestLog, e);
        } catch (TransactionStillProcessingException |
                 BetResultIdempotentViolationException e) {
            // this exception happened when handle repeated data
            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set balanceVo
            balanceVo.setValue(String.valueOf(balance.toBigInteger()));
            balanceVo.setVersion(String.valueOf(System.currentTimeMillis()));
            balanceVo.setType("real");
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());

            // set roundBetVo
            roundBetVo.setBalance(balanceVo);
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.RESULT_SUCCESS);

            // set vo
            vo.setRoundbet(roundBetVo);

            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException e) {

            if (e.getOperatorStatus().equals(Status.SC_INSUFFICIENT_FUNDS.code)) {
                // set errorVo
                errorVo.setCode(ResponseCodes.NOT_ENOUGH_MONEY);
                errorVo.setMsg(ResponseCodes.N_E_M);

                // set roundBetVo
                roundBetVo.setResult(ResponseCodes.RESULT_FAIL);

            } else {
                // set errorVo
                errorVo.setCode(ResponseCodes.WL_ERROR);
                errorVo.setMsg(ResponseCodes.WL_E);

                // set roundBetVo
                roundBetVo.setResult(ResponseCodes.RESULT_ERROR);

            }
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setError(errorVo);
            vo.setRoundbet(roundBetVo);

            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException |
                 JsonProcessingException |
                 VendorCurrencyNotSupportException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 CouchbaseDataIntegrityException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set roundBetVo
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.RESULT_ERROR);
            roundBetVo.setError(errorVo);

            // set vo
            vo.setRoundbet(roundBetVo);

            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.MAX_TIME_EXCEED);
            errorVo.setMsg(ResponseCodes.M_T_E);

            // set roundBetVo
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.RESULT_FAIL);
            roundBetVo.setError(errorVo);

            // set vo
            vo.setRoundbet(roundBetVo);

            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set roundBetVo
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.WL_ERROR);
            roundBetVo.setError(errorVo);

            // set vo
            vo.setRoundbet(roundBetVo);

            httpService.logError(httpRequestLog, e);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(transactionServiceDto, transactionServiceDto.getRoundbet().getWlid());
            }
            // set vo
            vo.setSession(transactionServiceDto.getSession());
            vo.setTime(transactionServiceDto.getTime());
        }

        return vo;
    }

    private void doValidation(TransactionServiceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRoundbet());

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRoundbet().getRoundnum());
    }

    private void doVerification(TransactionServiceDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, AuthenticationException, BetResultIdempotentViolationException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getRoundbet().getWlid(), InvalidPlayerException::new);

        // check existing round to avoid double deduct wallet balance in place bet
        if (!RoundBetStatus.FINISHED.code.equals(dto.getRoundbet().getFinished())) {
            vendorService.verifySettledRound(gameSession.getVendorPlayerId(), dto.getRoundbet().getRoundnum().getId());
        }
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) {
        BigDecimal balance = BigDecimal.ZERO;

        try {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

        } catch (Exception exception) {

        }

        return balance;
    }
}
