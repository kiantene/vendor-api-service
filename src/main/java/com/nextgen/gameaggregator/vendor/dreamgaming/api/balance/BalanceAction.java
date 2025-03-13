package com.nextgen.gameaggregator.vendor.dreamgaming.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.dreamgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.dreamgaming.vo.MemberVo;
import com.nextgen.gameaggregator.vendor.dreamgaming.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.CredentialException;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BalanceAction {
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final HttpService httpService;

    public BalanceAction(GameSessionService gameSessionService,
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

    @PostMapping(path = EndPoints.BALANCE)
    public ResponseVo getBalance(@PathVariable("agentName") String agentName,
                                 HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo responseVo = new ResponseVo();
        MemberVo memberVo = new MemberVo();
        String body = httpRequestLog.getRequestBody();
        try {
            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);

            // Get GameSession with username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getMember().getUsername().toLowerCase());

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            responseVo.setCodeMsg(ResponseCode.SUCCESS.code);

            memberVo.setUsername(balanceDto.getMember().getUsername());
            memberVo.setBalance(balance);

            responseVo.setMember(memberVo);
        } catch (JsonProcessingException | InvalidRequestException | CredentialException | NullPointerException |
                 AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setCodeMsg(ResponseCode.PARAMETER_ERROR.code);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setCodeMsg(ResponseCode.OPERATION_FAILED.code);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;

    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession)
            throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException, InvalidRequestException, CredentialNotFoundException, CredentialException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getMember().getUsername(), InvalidRequestException::new);

        //5. Verify received token is same with credential token md5(agent+apiKey)
        String agent = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_ID);
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        ValidationUtils.isEquals(VendorService.md5Generator(agent + apiKey), dto.getToken(), CredentialException::new);
    }
}
