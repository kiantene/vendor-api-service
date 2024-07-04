package com.nextgen.gameaggregator.vendor.ambslot.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ambslot.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ambslot.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ambslot.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ambslot.service.VendorService;
import com.nextgen.gameaggregator.vendor.ambslot.vo.StatusVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class BalanceAction {

    private final HttpService httpService;

    private final VendorLineService vendorLineService;

    private final AgentPlayerService agentPlayerService;

    private final VendorGameService vendorGameService;

    private final GameSessionService gameSessionService;

    private final WalletService walletService;

    @Autowired
    public BalanceAction(HttpService httpService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService,
                         GameSessionService gameSessionService,
                         WalletService walletService){
        this.httpService = httpService;

        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public BalanceVo balance(HttpServletRequest request){
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        BalanceVo balanceVo = new BalanceVo();
        DataVo dataVo = new DataVo();
        StatusVo statusVo = new StatusVo();

        try{
            String body = httpRequestLog.getRequestBody();

            BalanceDto balanceDto = HttpService.convertJsonToDto(body,BalanceDto.class);

            // get x-ambslot-signature value for validation
            String signature = httpService.getHeadersInfo(request).get(EndPoints.HEADER_SIGNATURE);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession, signature, body);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            dataVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            statusVo.setCode(ResponseCodes.SUCCESS);
            statusVo.setMessage(ResponseCodes.SUCCESS_MSG);

            balanceVo.setData(dataVo);

        }catch(InvalidRequestException |
               JsonProcessingException |
               InvalidPlayerException |
               AuthenticationException |
               InvalidSignatureException |
               CredentialNotFoundException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_REQUEST);
            statusVo.setMessage(ResponseCodes.INVALID_REQUEST_MSG);

        }catch(InvalidAgentApiCredentialException |
               VendorCurrencyNotSupportException |
               DisabledAgentPlayerException |
               DisabledGameException |
               InvalidOperatorResponseException |
               DisabledVendorLineException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_TIMEOUT_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_TIMEOUT_ERROR_MSG);

        }catch(InvalidCredentialsException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_AGENT);
            statusVo.setMessage(ResponseCodes.INVALID_AGENT_MSG);

        }catch(Exception e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_TIMEOUT_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_TIMEOUT_ERROR_MSG);

        }finally {
            balanceVo.setStatus(statusVo);
            httpService.end(httpRequestLog, balanceVo);
        }

        return balanceVo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession, String signature, String body) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, InvalidRequestException, CredentialNotFoundException, InvalidSignatureException, JsonProcessingException, InvalidCredentialsException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), InvalidPlayerException::new);

        // Verify received agentId is same with credential
        String agentId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.prefix);
        ValidationUtils.isEquals(agentId.toLowerCase(), dto.getAgentId(), InvalidCredentialsException::new);

        // Verify header value
        String secret = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.secret);
        VendorService.validateSignature(signature, body, secret);
    }
}
