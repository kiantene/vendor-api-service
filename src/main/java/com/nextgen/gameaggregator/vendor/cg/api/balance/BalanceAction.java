package com.nextgen.gameaggregator.vendor.cg.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import com.nextgen.gameaggregator.vendor.cg.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;


    @Autowired
    public BalanceAction(HttpService httpService,
                         GameSessionService gameSessionService,
                         WalletService walletService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo balanceVo = new ResponseVo();
        try {
            //convert body into dto
            BalanceDto dto = HttpService.convertQueryStringToDtoUrlDecode(httpRequestLog, BalanceDto.class);

            //basic validation
            this.doValidation(dto);

            //get game session
            //Authentication error throw if session not found
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAccountId());

            //basic verification
            this.doVerification(dto, gameSession);

            //get player wallet balance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            //set values
            balanceVo.setChannelId(dto.getChannelId());
            balanceVo.setAccountId(dto.getAccountId());
            balanceVo.setBalance(balance);
            balanceVo.setCurrency(gameSession.getVendorCurrencyCode());
            balanceVo.setErrorCode(ResponseCodes.SUCCESS);
            balanceVo.setReturnTime(VendorService.returnTime());
        } catch (InvalidVendorLineException e) {
            balanceVo.setErrorCode(ResponseCodes.CHANNEL_ID_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            balanceVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_PLAYER);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            balanceVo.setErrorCode(ResponseCodes.SEAMLESS_INPUT_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            balanceVo.setErrorCode(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, balanceVo);
        }
        return balanceVo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws CredentialNotFoundException, InvalidVendorLineException, InvalidRequestException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {
        //verify vendor's channel id
        String channelId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CHANNEL_ID);
        ValidationUtils.isEquals(channelId, dto.getChannelId(), InvalidVendorLineException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
