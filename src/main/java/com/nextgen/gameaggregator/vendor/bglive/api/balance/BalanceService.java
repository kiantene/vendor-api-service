package com.nextgen.gameaggregator.vendor.bglive.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
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

    public CommonVo balance(HttpRequestLog httpRequestLog, String traceId) {

        CommonVo commonVo = new CommonVo();

        try {
            String body = httpRequestLog.getRequestBody();
            CommonDto commonDto = HttpService.convertJsonToDto(body, CommonDto.class);
            // Handle the action and return the resulting value
            this.doValidation(commonDto);

            String vendorPlayerLoginId = commonDto.getCommonParamsDto().getLoginId();
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorPlayerLoginId);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());
            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, gameSession);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal getWalletBalance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 6. Set response data
            commonVo.setSuccessResponse(commonDto.getId(), getWalletBalance);

        } catch (InvalidRequestException e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.AUTH_INVALID.code,
                    ResponseCodes.AUTH_INVALID.message, ResponseCodes.AUTH_INVALID.message);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.PLAYER_INVALID.code,
                    ResponseCodes.PLAYER_INVALID.message, ResponseCodes.PLAYER_INVALID.message);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);
        }
        return commonVo;
    }


    private void doValidation(CommonDto commonDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonDto);

    }

    private void doVerification(CommonDto commonDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidFormatException {

        String snCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SN_CODE);
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(snCode, commonDto.getCommonParamsDto().getSn(), InvalidPlayerException::new);

        String validateSign = VendorService.encryptLoginMd5Key(commonDto.getCommonParamsDto().getRandom(), snCode,
                gameSession.getVendorPlayerUsername(), secretKey);
        ValidationUtils.isEquals(validateSign, commonDto.getCommonParamsDto().getSign(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}