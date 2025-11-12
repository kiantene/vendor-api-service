package com.nextgen.gameaggregator.vendor.cg.api.balance;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cg.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import com.nextgen.gameaggregator.vendor.cg.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    private final VendorService vendorService;


    @Autowired
    public BalanceAction(HttpService httpService,
                         GameSessionService gameSessionService,
                         WalletService walletService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService,
                         VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public String balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo balanceVo = new ResponseVo();
        CommonDto dto = new CommonDto();
        try {
            //convert body into dto
            dto = HttpService.convertQueryStringToDto(httpRequestLog, CommonDto.class);
            dto.setData(VendorService.urlDecode(dto.getData()));

            //basic validation
            this.doValidation(dto);

            //decrypt token return from vendor
            String decryptedData = vendorService.decryptData(dto.getData(), dto.getChannelId());//we get the json here
            httpRequestLog.setRequestBody(decryptedData);
            BalanceDto balanceDto = HttpService.convertJsonToDto(decryptedData, BalanceDto.class);

            //get game session
            //Authentication error throw if session not found
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getAccountId());

            //basic verification
            this.doVerification(balanceDto, gameSession);

            //get player wallet balance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            //set values
            balanceVo.setChannelId(balanceDto.getChannelId());
            balanceVo.setAccountId(balanceDto.getAccountId());
            balanceVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
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
            try {
                String jsonString = new Gson().toJson(balanceVo);
                balanceVo.setEncrypt(vendorService.encryptResponse(jsonString, dto.getChannelId())); //encrypt the whole vo include error
                httpService.end(httpRequestLog, balanceVo);
            } catch (CredentialNotFoundException e) {
                httpService.logError(httpRequestLog, e);
            }
        }
        return balanceVo.getEncrypt();
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
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
