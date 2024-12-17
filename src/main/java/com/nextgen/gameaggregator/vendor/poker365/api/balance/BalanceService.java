package com.nextgen.gameaggregator.vendor.poker365.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.constant.Credentials;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.poker365.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BalanceService {

    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;
    private final HttpService httpService;

    Integer vendorPlayerId;

    @Autowired
    public BalanceService(
            WalletService walletService,
            VendorService vendorService,
            GameSessionService gameSessionService,
            VendorLineService vendorLineService,
            AgentPlayerService agentPlayerService, VendorPlayerService vendorPlayerService, HttpService httpService) {
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
        this.httpService = httpService;
    }
    
    public CommonVo balance(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);
            // 2. Validate request parameters (Non-database calls)
            this.doValidation(balanceDto);


            this.vendorPlayerId = Integer.valueOf(balanceDto.getMessage().getUserId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(vendorPlayerId), null);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal getWalletBalance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 6. Set response data
            commonVo.setBalance(getWalletBalance);
            commonVo.setStatus(ResponseCodes.SUCCESS_200.status);

//        } catch (InvalidPlayerException e) {
//            balanceVo.setError(ErrorVo.from(ResponseCodes.ERR_PLAYER_NOT_FOUND));
//            httpService.logError(httpRequestLog, e);
//        } catch (AuthenticationException e) {
//            balanceVo.setError(ErrorVo.from(ResponseCodes.ERR_AUTHENTICATION_FAILED));
//            httpService.logError(httpRequestLog, e);
//        } catch (InvalidRequestException e) {
//            balanceVo.setError(ErrorVo.from(ResponseCodes.ERR_REGULATORY_GENERAL));
//            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            commonVo.setStatus(ResponseCodes.FAIL.status);
            commonVo.setMsg(ResponseCodes.FAIL.message);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(BalanceDto balanceDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(balanceDto);
    }

    private void doVerification(BalanceDto balanceDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException, DisabledAgentPlayerException, CredentialNotFoundException, InvalidVendorLineException, InvalidPlayerException, CredentialNotFoundException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String cert = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CERT);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(cert, balanceDto.getKey(), InvalidPlayerException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerId()), balanceDto.getMessage().getUserId(), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}
