package com.nextgen.gameaggregator.vendor.habanero.api.authenticate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.constant.Credentials;
import com.nextgen.gameaggregator.vendor.habanero.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
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
public class AuthAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    @PostMapping(path = EndPoints.AUTHENTICATE)
    public AuthVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        AuthVo responseVo = new AuthVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into authDto
            AuthDto authDto = HttpService.convertJsonToDto(body, AuthDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(authDto);

            //Get GameSession by token
            GameSession gameSession = gameSessionService.verifyToken(authDto.getPlayerDetailRequest().getToken());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(authDto, gameSession);

            //Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            //return success respond
            responseVo.getPlayerDetailResponseVo().setAccountId(gameSession.getVendorPlayerUsername());
            responseVo.getPlayerDetailResponseVo().setAccountnName(gameSession.getVendorPlayerUsername());
            responseVo.getPlayerDetailResponseVo().setBalance(balance.setScale(2, RoundingMode.DOWN));
            responseVo.getPlayerDetailResponseVo().setCurrencyCode(gameSession.getVendorCurrencyCode());

        } catch (InvalidAgentApiCredentialException |
                 AuthenticationException |
                 InvalidRequestException |
                 NoAvailableLineException |
                 JsonProcessingException |
                 CredentialNotFoundException |
                 DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 DisabledGameException generalException) {
            responseVo.setResponseCode(ResponseCodes.AUTHENTICATE_ERROR);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setResponseCode(ResponseCodes.AUTHENTICATE_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.AUTHENTICATE_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);

        }

        return responseVo;

    }

    private void doValidation(AuthDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getBaseGame());
        ValidationUtils.validateRequest(dto.getSubAuth());
        ValidationUtils.validateRequest(dto.getPlayerDetailRequest());
        ValidationUtils.isEquals("playerdetailrequest", dto.getType(), InvalidRequestException::new);
    }

    private void doVerification(AuthDto dto, GameSession gameSession) throws
            NoAvailableLineException,
            CredentialNotFoundException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException {

        //Verify received passkey is the same from credential
        String passkey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PASSKEY);
        ValidationUtils.isEquals(passkey, dto.getSubAuth().getPasskey(), NoAvailableLineException::new);

        //Verify received brand id is the same from credential
        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        ValidationUtils.isEquals(brandId, dto.getSubAuth().getBrandid(), NoAvailableLineException::new);

        //Verify vendor game code is the same from gameSession
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getBaseGame().getKeyName(), NoAvailableLineException::new);

        //Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        //Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        //Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
