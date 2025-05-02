package com.nextgen.gameaggregator.vendor.ifg.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ifg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ifg.service.VendorService;
import com.nextgen.gameaggregator.vendor.ifg.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.ErrorVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CreditService {

    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public CreditService(GameSessionService gameSessionService,
                         WalletService walletService,
                         HttpService httpService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService,
                         VendorService vendorService,
                         VendorLineService vendorLineService,
                         RequestIdempotentLogService requestIdempotentLogService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }


    public CommonVo credit(HttpRequestLog httpRequestLog, String traceId) {
        CreditServiceDto creditServiceDto = new CreditServiceDto();
        CreditServiceVo vo = new CreditServiceVo();
        BalanceVo balanceVo = new BalanceVo();
        RoundWinVo roundWinVo = new RoundWinVo();
        ErrorVo errorVo = new ErrorVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();
        BigDecimal balance;
        boolean isRequestExists = false;
        try {
            creditServiceDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), CreditServiceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(creditServiceDto);
            // Request idempotent checking.
            if (requestIdempotentLogService.checkExists(creditServiceDto, creditServiceDto.getRoundWinDto().getWlid()) == null) {
                requestIdempotentLogService.create(creditServiceDto, creditServiceDto.getRoundWinDto().getWlid());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }
            // Verify session token
            gameSession = gameSessionService.verifyToken(creditServiceDto.getRoundWinDto().getGuid());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditServiceDto, gameSession);

       
            ResultType resultType = vendorService.checkResult(creditServiceDto.getRoundWinDto().getWin(), creditServiceDto.getRoundWinDto().getFinished());
            balance = walletService.processBetResult(traceId, gameSession, creditServiceDto, resultType, vendorService, httpRequestLog);
            Thread.sleep(9000);
            // set balanceVo
            balanceVo.setVersion(String.valueOf(System.currentTimeMillis()));
            balanceVo.setValue(String.valueOf(balance.intValue()));
            balanceVo.setType("real");
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());

            // set roundWinVo
            roundWinVo.setBalanceVo(balanceVo);
            roundWinVo.setResult(ResponseCodes.RESULT_SUCCESS);
            roundWinVo.setId(creditServiceDto.getRoundWinDto().getId());

            // set vo
            vo.setRoundwin(roundWinVo);
        } catch (VendorCurrencyNotSupportException |
                 AuthenticationException |
                 InsufficientBalanceException |
                 InvalidOperatorResponseException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 MergedBetDataIntegrityException |
                 DisabledGameException |
                 InvalidRequestException |
                 BetNotFoundException |
                 JsonProcessingException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set roundWinVo
            roundWinVo.setId(creditServiceDto.getRoundWinDto().getId());
            roundWinVo.setResult(ResponseCodes.RESULT_ERROR);
            roundWinVo.setError(errorVo);

            // set vo
            vo.setRoundwin(roundWinVo);

            httpService.logError(httpRequestLog, e);
        } catch (TransactionStillProcessingException |
                 BetResultIdempotentViolationException e) {
            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set balanceVo
            balanceVo.setVersion(String.valueOf(System.currentTimeMillis()));
            balanceVo.setValue(String.valueOf(balance.intValue()));
            balanceVo.setType("real");
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());

            // set roundWinVo
            roundWinVo.setBalanceVo(balanceVo);
            roundWinVo.setResult(ResponseCodes.RESULT_SUCCESS);
            roundWinVo.setId(creditServiceDto.getRoundWinDto().getId());

            // set vo
            vo.setRoundwin(roundWinVo);

            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set roundWinVo
            roundWinVo.setId(creditServiceDto.getRoundWinDto().getId());
            roundWinVo.setResult(ResponseCodes.RESULT_ERROR);
            roundWinVo.setError(errorVo);

            // set vo
            vo.setRoundwin(roundWinVo);

            httpService.logError(httpRequestLog, e);
        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(creditServiceDto, creditServiceDto.getRoundWinDto().getWlid());
            }
            // set vo
            vo.setSession(creditServiceDto.getSession());
            vo.setTime(creditServiceDto.getTime());
        }

        return vo;
    }

    private void doValidation(CreditServiceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRoundWinDto());

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRoundWinDto().getRoundNumDto());
    }

    private void doVerification(CreditServiceDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            AuthenticationException, BetResultIdempotentViolationException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getRoundWinDto().getWlid(), InvalidPlayerException::new);
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
