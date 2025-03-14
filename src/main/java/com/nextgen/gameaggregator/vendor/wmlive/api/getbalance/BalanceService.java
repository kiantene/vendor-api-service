package com.nextgen.gameaggregator.vendor.wmlive.api.getbalance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.wmlive.api.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.wmlive.api.vo.DataVo;
import com.nextgen.gameaggregator.vendor.wmlive.api.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.wmlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.wmlive.constant.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.CredentialException;
import java.math.BigDecimal;

@Service
public class BalanceService {
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final HttpService httpService;

    @Autowired
    public BalanceService(GameSessionService gameSessionService,
                          WalletService walletService,
                          VendorLineService vendorLineService,
                          AgentPlayerService agentPlayerService,
                          VendorGameService vendorGameService,
                          HttpService httpService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.httpService = httpService;
    }

    public ResponseVo getBalance(String traceId, HttpRequestLog httpRequestLog) {
        ResponseVo responseVo = new ResponseVo();

        String body = httpRequestLog.getRequestBody();
        try {
            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);

            // Get GameSession with token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(commonDto.getUser());

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            DataVo balanceDataVo = new DataVo(commonDto, balance);
            responseVo.setResult(balanceDataVo);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            responseVo.setResponseCodeMsg(ResponseCode.ERROR_NOT_AUTHORIZED);
        } catch (CredentialNotFoundException | CredentialException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.CREDENTIAL_ERROR);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCodeMsg(ResponseCode.ERROR);

        }

        return responseVo;

    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CommonDto dto, GameSession gameSession)
            throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException, InvalidRequestException, CredentialNotFoundException, CredentialException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUser(), InvalidRequestException::new);

        //5. Verify received signature is same with credential signature
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SIGNATURE);
        ValidationUtils.isEquals(token, dto.getSignature(), CredentialException::new);
    }
}
