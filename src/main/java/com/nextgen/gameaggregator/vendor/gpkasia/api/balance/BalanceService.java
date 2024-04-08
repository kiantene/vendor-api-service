package com.nextgen.gameaggregator.vendor.gpkasia.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import com.nextgen.gameaggregator.vendor.gpkasia.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Credentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public CommonVo balance(HttpRequestLog httpRequestLog, String traceId) {
        BalanceDto balanceDto = new BalanceDto();
        CommonVo vo = new CommonVo();
        BalanceDataVo dataVo = new BalanceDataVo();

        try{
            balanceDto = httpService.convertQueryStringToDto(httpRequestLog.getRequestBody(), BalanceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getUser());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            vo.setCodeMsg(ResponseCodes.SUCCESS);

            dataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());
            dataVo.setUser(balanceDto.getUser());
            dataVo.setTimestamp(String.valueOf(vendorService.getCurrentTime()));

            vo.setData(dataVo);

        } catch (InvalidRequestException |
                 AuthenticationException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 DisabledVendorLineException |
                 CredentialNotFoundException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR);
        }catch(Exception e){
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR);
        }

        return vo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, InvalidRequestException, CredentialNotFoundException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUser(), InvalidPlayerException::new);

        //Verify received api_token is same with credential
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.api_token);
        ValidationUtils.isEquals(token, dto.getApi_token(), InvalidRequestException::new);
    }
}
