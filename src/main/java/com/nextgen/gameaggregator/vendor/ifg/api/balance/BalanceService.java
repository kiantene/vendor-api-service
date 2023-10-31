package com.nextgen.gameaggregator.vendor.ifg.api.balance;

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
public class BalanceService {

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

    public CommonVo balance(HttpRequestLog httpRequestLog, String traceId){
        BalanceServiceDto balanceDto = new BalanceServiceDto();

        // Construct VO
        BalanceServiceVo vo = new BalanceServiceVo();
        ErrorVo errorVo = new ErrorVo();
        XmlMapper xmlMapper = new XmlMapper();
        GetbalanceVo getbalanceVo = new GetbalanceVo();
        BalanceVo balanceVo = new BalanceVo();
        GameSession gameSession = new GameSession();

        try{
            balanceDto = xmlMapper.readValue(httpRequestLog.getRequestBody(),BalanceServiceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(balanceDto.getGetbalance().getGuid());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // set balanceVo
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());
            balanceVo.setType("real");
            balanceVo.setValue(String.valueOf(balance.intValue()));
            balanceVo.setVersion(String.valueOf(System.currentTimeMillis()));

            // set getbalanceVo
            getbalanceVo.setBalance(balanceVo);
            getbalanceVo.setId(balanceDto.getGetbalance().getId());
            getbalanceVo.setResult(ResponseCodes.RESULT_SUCCESS);

            // set vo
            vo.setGetbalanceVo(getbalanceVo);

        } catch (InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 VendorCurrencyNotSupportException |
                 AuthenticationException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 InvalidOperatorResponseException |
                 JsonProcessingException |
                 DisabledVendorLineException e) {
            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set getbalanceVo
            getbalanceVo.setId(balanceDto.getGetbalance().getId());
            getbalanceVo.setResult(ResponseCodes.RESULT_ERROR);
            getbalanceVo.setError(errorVo);

            // set vo
            vo.setGetbalanceVo(getbalanceVo);

            httpService.logError(httpRequestLog, e);
        } catch(Exception e){
            // set errorVo
            errorVo.setCode(ResponseCodes.WL_ERROR);
            errorVo.setMsg(ResponseCodes.WL_E);

            // set getbalanceVo
            getbalanceVo.setId(balanceDto.getGetbalance().getId());
            getbalanceVo.setResult(ResponseCodes.RESULT_ERROR);
            getbalanceVo.setError(errorVo);

            // set vo
            vo.setGetbalanceVo(getbalanceVo);

            httpService.logError(httpRequestLog, e);
        } finally{
            // set vo
            vo.setSession(balanceDto.getSession());
            vo.setTime(balanceDto.getTime());
        }

        return vo;
    }

    private void doValidation(BalanceServiceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getGetbalance());
    }

    private void doVerification(BalanceServiceDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, AuthenticationException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getGetbalance().getWlid(), InvalidPlayerException::new);

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());
    }
}
