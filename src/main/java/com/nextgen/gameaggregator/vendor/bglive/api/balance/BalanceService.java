package com.nextgen.gameaggregator.vendor.bglive.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class BalanceService {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;


    @Autowired
    public BalanceService(HttpService httpService,
                          VendorLineService vendorLineService,
                          AgentPlayerService agentPlayerService,
                          GameSessionService gameSessionService,
                          WalletService walletService,
                          VendorPlayerService vendorPlayerService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorPlayerService = vendorPlayerService;
    }

    public Map<String, Object> balance(HttpRequestLog httpRequestLog, String traceId) {

//        CommonVo commonVo = new CommonVo();
//
//        try {
//            String body = httpRequestLog.getRequestBody();
//            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);
//            // Handle the action and return the resulting value
//            this.doValidation(balanceDto);
//
//            String vendorPlayerLoginId = balanceDto.getParams().getLoginId();
//            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorPlayerLoginId);
//            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());
//            // 4. Verify remaining parameters (Verify against database values)
//            this.doVerification(balanceDto, gameSession);
//
//            // 5. Retrieve the latest wallet balance from Operator
//            BigDecimal getWalletBalance = walletService.getBalance(traceId, gameSession, httpRequestLog);
//
//            // 6. Set response data
//            commonVo.setId(balanceDto.getId());
//            commonVo.setResult(getWalletBalance);
        Map<String, Object> response = new HashMap<>();
        response.put("id", "001");
        response.put("result", 888.66);
        response.put("error", null);
        response.put("jsonrpc", 2.0);

        return response;

//        } catch (InvalidRequestException e) {
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
//                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
//            httpService.logError(httpRequestLog, e);
//        } catch (AuthenticationException e) {
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.AUTH_INVALID.code,
//                    ResponseCodes.AUTH_INVALID.message, ResponseCodes.AUTH_INVALID.message);
//            httpService.logError(httpRequestLog, e);
//        } catch (InvalidPlayerException e) {
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.PLAYER_INVALID.code,
//                    ResponseCodes.PLAYER_INVALID.message, ResponseCodes.PLAYER_INVALID.message);
//            httpService.logError(httpRequestLog, e);
//
//        } catch (Exception e) {
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
//                    ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
//            httpService.logError(httpRequestLog, e);
//        }
//        return commonVo;
    }


    private void doValidation(BalanceDto balanceDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(balanceDto);

    }

    private void doVerification(BalanceDto balanceDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            InvalidVendorLineException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidFormatException {

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String snCode = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.SN_CODE);
        String secretKey = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.API_KEY);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(snCode, balanceDto.getParams().getSn(), InvalidPlayerException::new);

        String validateSign = VendorService.encryptLoginMd5Key(balanceDto.getParams().getRandom(), snCode,
                gameSession.getVendorPlayerUsername(), secretKey);
        ValidationUtils.isEquals(validateSign, balanceDto.getParams().getSign(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}