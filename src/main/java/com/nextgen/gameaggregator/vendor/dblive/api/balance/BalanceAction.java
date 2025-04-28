package com.nextgen.gameaggregator.vendor.dblive.api.balance;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dblive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dblive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dblive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dblive.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.dblive.service.VendorService;
import com.nextgen.gameaggregator.vendor.dblive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.UnexpectedTypeException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.dblive.service.VendorService.convertDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BalanceAction {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final ObjectMapper objectMapper;

    public BalanceAction(HttpService httpService, VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService, VendorGameService vendorGameService,
                         GameSessionService gameSessionService, WalletService walletService, ObjectMapper objectMapper) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();
        String md5Key = "";
        try {
            String body = httpRequestLog.getRequestBody();

            //convert queryString to dto
            CommonDto balanceDto = HttpService.convertJsonToDto(body, CommonDto.class);
            this.doValidation(balanceDto);

            //convert and validate request param
            BalanceParamsDto balanceParamsDto = VendorService.convertDto(balanceDto.getParams(), BalanceParamsDto.class);
            this.doValidation(balanceParamsDto);

            String vendorPlayerUsername = VendorService.extractVendorPlayerUsername(balanceParamsDto.getLoginName());

            // using vendorPlayerId to find gameSession details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayerUsername);

            //Verification
            this.doVerification(balanceDto, gameSession, vendorPlayerUsername);
            md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            //Encrypt Data
            BalanceDataVo balanceDataVo = new BalanceDataVo();
            balanceDataVo.setLoginName(balanceParamsDto.getLoginName());
            balanceDataVo.setBalance(convertDecimal(balance));

            String signature = generateSignature(balanceDataVo, md5Key);
            responseVo.setResponseSuccess(balanceDataVo, signature);
        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCodes.INVALID_PLAYER_SESSION);
        } catch (DisabledGameException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCodes.INVALID_GAME_ID);
        } catch (CredentialNotFoundException | JsonParseException |
                 UnexpectedTypeException | InvalidPlayerException | InvalidRequestException
                e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
        } catch (InvalidSignatureException exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);
        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setResponseCode(ResponseCodes.OTHER_ERROR);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private <T> void doValidation(T requestObject) throws InvalidRequestException {
        // Validation with custom exception
        ValidationUtils.validateRequest(requestObject);
    }

    private String generateSignature(BalanceDataVo balanceDataVo, String md5Key) throws JsonProcessingException {
        return VendorService.getMD5(objectMapper.writeValueAsString(balanceDataVo) + md5Key);
    }

    private void doVerification(CommonDto balanceDto, GameSession gameSession, String vendorPlayerUsername) throws
            InvalidPlayerException, DisabledVendorLineException, CredentialNotFoundException,
            DisabledAgentPlayerException, DisabledGameException, InvalidSignatureException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        //Verify Signature is match
        String md5Key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SEAMLESS_MD5_KEY);
        VendorService.verifySignature(balanceDto.getParams(), md5Key, balanceDto.getSignature());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), vendorPlayerUsername, InvalidPlayerException::new);
    }
}
