package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;

@Service
@Slf4j
public class BalanceService {
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;

    @Autowired
    public BalanceService(GameSessionService gameSessionService,
                          VendorLineService vendorLineService,
                          WalletService walletService,
                          HttpService httpService,
                          AgentPlayerService agentPlayerService,
                          VendorGameService vendorGameService) {
        this.gameSessionService = gameSessionService;

        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
    }

    public CommonVo balance(HttpRequestLog httpRequestLog, String traceId) {
        BalanceDto balanceDto = new BalanceDto();
        CommonVo vo = new CommonVo();
        BalanceDataVo dataVo = new BalanceDataVo();

        try {
            balanceDto = HttpService.convertQueryStringToDto(URLDecoder.decode(httpRequestLog.getRequestBody(), "UTF-8"), BalanceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getUser());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            vo.setCodeMsg(ResponseCodes.SUCCESS.code);

            dataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());
            dataVo.setUser(balanceDto.getUser());
            dataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));

            vo.setData(dataVo);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR.code);
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
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_TOKEN);
        ValidationUtils.isEquals(token, dto.getApiToken(), InvalidRequestException::new);
    }
}
