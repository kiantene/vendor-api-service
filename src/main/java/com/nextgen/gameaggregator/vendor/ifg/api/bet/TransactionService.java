package com.nextgen.gameaggregator.vendor.ifg.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ifg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ifg.service.VendorService;
import com.nextgen.gameaggregator.vendor.ifg.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.ifg.vo.ErrorVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class TransactionService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;

    public CommonVo transaction(HttpRequestLog httpRequestLog, String traceId){
        TransactionServiceDto transactionServiceDto = new TransactionServiceDto();
        TransactionServiceVo vo = new TransactionServiceVo();
        BalanceVo balanceVo = new BalanceVo();
        RoundBetVo roundBetVo = new RoundBetVo();
        ErrorVo errorVo = new ErrorVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();
        BigDecimal balance = null;

        try{
            transactionServiceDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), TransactionServiceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(transactionServiceDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(transactionServiceDto.getRoundbet().getGuid());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(transactionServiceDto, gameSession);

            // Process Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, transactionServiceDto, httpRequestLog.getRequestBody(), httpRequestLog);

            // set balanceVo
            balanceVo.setValue(String.valueOf(betEvent.getLastBalance().intValue()));
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
        } catch (TransactionStillProcessingException |
                 BetResultIdempotentViolationException e) {
            // this exception happened when handle repeated data
            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set balanceVo
            balanceVo.setValue(String.valueOf(balance.intValue()));
            balanceVo.setVersion(String.valueOf(System.currentTimeMillis()));
            balanceVo.setType("real");
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());

            // set roundBetVo
            roundBetVo.setBalance(balanceVo);
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.RESULT_SUCCESS);

            // set vo
            vo.setRoundbet(roundBetVo);
        } catch(InvalidRequestException |
                JsonProcessingException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set roundBetVo
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.RESULT_ERROR);
            roundBetVo.setError(errorVo);

            // set vo
            vo.setRoundbet(roundBetVo);
        } catch (VendorCurrencyNotSupportException |
                 AuthenticationException |
                 InvalidOperatorResponseException |
                 CouchbaseDataIntegrityException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 DisabledGameException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.MAX_TIME_EXCEED);
            errorVo.setMsg(ResponseCodes.M_T_E);

            // set roundBetVo
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.RESULT_FAIL);
            roundBetVo.setError(errorVo);

            // set vo
            vo.setRoundbet(roundBetVo);
        }  catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);

            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set roundBetVo
            roundBetVo.setId(transactionServiceDto.getRoundbet().getId());
            roundBetVo.setResult(ResponseCodes.WL_ERROR);
            roundBetVo.setError(errorVo);

            // set vo
            vo.setRoundbet(roundBetVo);
        } finally{
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

    private void doVerification(TransactionServiceDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, AuthenticationException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getRoundbet().getWlid(), InvalidPlayerException::new);
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
