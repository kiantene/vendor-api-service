package com.nextgen.gameaggregator.vendor.ifg.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
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
public class RollBackService {

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

    public CommonVo rollback(HttpRequestLog httpRequestLog, String traceId){
        RollBackServiceDto rollBackServiceDto = new RollBackServiceDto();
        RollBackServiceVo vo = new RollBackServiceVo();
        ErrorVo errorVo = new ErrorVo();
        RefundVo refundVo = new RefundVo();
        BalanceVo balanceVo = new BalanceVo();
        XmlMapper xmlMapper = new XmlMapper();
        GameSession gameSession = new GameSession();
        BigDecimal balance = null;

        try{
            rollBackServiceDto = xmlMapper.readValue(httpRequestLog.getRequestBody(), RollBackServiceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(rollBackServiceDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(rollBackServiceDto.getRefund().getGuid());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollBackServiceDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            balance = walletService.processRollback(traceId, rollBackServiceDto, gameSession, vendorService, httpRequestLog);

            // set balanceVo
            balanceVo.setVersion(String.valueOf(System.currentTimeMillis()));
            balanceVo.setValue(String.valueOf(balance.intValue()));
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());
            balanceVo.setType("real");

            // set refundVo
            refundVo.setBalance(balanceVo);
            refundVo.setId(rollBackServiceDto.getRefund().getId());
            refundVo.setResult(ResponseCodes.RESULT_SUCCESS);

            // set vo
            vo.setRefund(refundVo);
        } catch (VendorCurrencyNotSupportException |
                 AuthenticationException |
                 GameNotSupportedException |
                 InvalidOperatorResponseException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 RecordNotFoundException |
                 JsonProcessingException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set refundVo
            refundVo.setError(errorVo);
            refundVo.setId(rollBackServiceDto.getRefund().getId());
            refundVo.setResult(ResponseCodes.RESULT_ERROR);

            // set vo
            vo.setRefund(refundVo);
        } catch (BetResultIdempotentViolationException |
                 BetNotFoundException |
                 BetRefundIdempotentViolationException |
                 TransactionStillProcessingException e) {
            // this exception happened when handle repeated data or data was not found
            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set balanceVo
            balanceVo.setVersion(String.valueOf(System.currentTimeMillis()));
            balanceVo.setValue(String.valueOf(balance.intValue()));
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());
            balanceVo.setType("real");

            // set refundVo
            refundVo.setBalance(balanceVo);
            refundVo.setId(rollBackServiceDto.getRefund().getId());
            refundVo.setResult(ResponseCodes.RESULT_SUCCESS);

            // set vo
            vo.setRefund(refundVo);
        } catch(Exception exception) {
            httpService.logError(httpRequestLog, exception);

            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set refundVo
            refundVo.setError(errorVo);
            refundVo.setId(rollBackServiceDto.getRefund().getId());
            refundVo.setResult(ResponseCodes.RESULT_ERROR);

            // set vo
            vo.setRefund(refundVo);
        } finally{
            vo.setSession(rollBackServiceDto.getSession());
            vo.setTime(rollBackServiceDto.getTime());
        }

        return vo;
    }

    private void doValidation(RollBackServiceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRefund());

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRefund().getStorno());

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getRefund().getStorno().getRoundnum());
    }

    private void doVerification(RollBackServiceDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException, InvalidPlayerException, GameNotSupportedException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        // Verify player username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getRefund().getWlid(), InvalidPlayerException::new);

        // Verify player username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getRefund().getStorno().getWlid(), InvalidPlayerException::new);

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getRefund().getStorno().getGameid(), GameNotSupportedException::new);
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
