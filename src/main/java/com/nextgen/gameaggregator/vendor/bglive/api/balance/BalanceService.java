package com.nextgen.gameaggregator.vendor.bglive.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BalanceService {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;


    private String vendorPlayerLoginId;

    @Autowired
    public BalanceService(HttpService httpService,
                          VendorLineService vendorLineService,
                          AgentPlayerService agentPlayerService,
                          VendorGameService vendorGameService,
                          GameSessionService gameSessionService,
                          WalletService walletService, VendorPlayerService vendorPlayerService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorPlayerService = vendorPlayerService;
    }

    public CommonVo balance(HttpRequestLog httpRequestLog, String traceId) {

        CommonVo commonVo = new CommonVo();

        try {
            String body = httpRequestLog.getRequestBody();
            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);
            // Handle the action and return the resulting value
            this.doValidation(balanceDto);

            this.vendorPlayerLoginId = balanceDto.getParams().getLoginId();
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorPlayerLoginId);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());
//
            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal getWalletBalance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 6. Set response data
            commonVo.setId(balanceDto.getId());
            commonVo.setResult(getWalletBalance);

        } catch (JsonProcessingException | InvalidRequestException | InvalidPlayerException e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.ERROR.code, ResponseCodes.ERROR.message, ResponseCodes.ERROR.message);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException | DisabledVendorLineException | DisabledAgentPlayerException |
                 InvalidVendorLineException | CredentialNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvalidAgentApiCredentialException e) {
            throw new RuntimeException(e);
        } catch (VendorCurrencyNotSupportException e) {
            throw new RuntimeException(e);
        } catch (InvalidOperatorResponseException e) {
            throw new RuntimeException(e);
        }

        return commonVo;
    }


    private void doValidation(BalanceDto balanceDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(balanceDto);

    }

    private void doVerification(BalanceDto balanceDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException, DisabledAgentPlayerException, InvalidVendorLineException, InvalidPlayerException, CredentialNotFoundException {

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String snCode = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.SN_CODE);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(snCode, balanceDto.getParams().getSn(), InvalidPlayerException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerUsername()), balanceDto.getParams().getLoginId(), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}