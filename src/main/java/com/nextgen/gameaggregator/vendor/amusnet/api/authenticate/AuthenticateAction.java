package com.nextgen.gameaggregator.vendor.amusnet.api.authenticate;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.amusnet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.amusnet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.amusnet.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.amusnet.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthenticateAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final WalletService walletService;
    private final VendorService vendorService;

    @Autowired
    public AuthenticateAction(HttpService httpService,
                              GameSessionService gameSessionService,
                              VendorLineService vendorLineService,
                              AgentPlayerService agentPlayerService,
                              VendorGameService vendorGameService,
                              WalletService walletService,
                              VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.walletService = walletService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.AUTHENTICATE)
    public String authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        AuthenticateDto authenticateDto;
        AuthenticateVo vo = new AuthenticateVo();
        XmlMapper xmlMapper = new XmlMapper();
        String traceId = httpRequestLog.getId();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            authenticateDto = xmlMapper.readValue(body, AuthenticateDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(authenticateDto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(authenticateDto.getPlayerId());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(authenticateDto.getVendorGameId(), gameSession);

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(authenticateDto, gameSession);

            // Regenerate token for session token (launch token only can be use once time)
            gameSessionService.regenerateVendorToken(gameSession, UUID.randomUUID().toString());

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 5. Set response data
            vo.setBalance(balance.toBigInteger());
            vo.setAuthenticationToken(gameSession.getTraceId());
            vo.setResponseCodes(ResponseCodes.OK);

        } catch (InvalidTokenException exception) { // any other exception encountered
            httpService.logError(httpRequestLog, exception);
            vo.setResponseCodes(ResponseCodes.DEFENCE_CODE_ERROR);
            // Serialize the object to XML in string format
        } catch (Exception exception) { // any other exception encountered
            httpService.logError(httpRequestLog, exception);
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);
            // Serialize the object to XML in string format
        } finally {
            vendorService.buildResponseVo(vo);
            httpService.end(httpRequestLog, vo);
        }
        return vo.getResponseXMLFormat();

    }

    private void doValidation(AuthenticateDto authenticateDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(authenticateDto);
    }

    private void doVerification(AuthenticateDto authenticateDto, GameSession gameSession) throws InvalidTokenException,
            DisabledVendorLineException, InvalidRequestException, DisabledAgentPlayerException, DisabledGameException, CredentialNotFoundException {
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), authenticateDto.getPlayerId(), InvalidRequestException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        String userName = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        String password = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PASSWORD);
        String portalCodeEQ = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_CODE_EQ);
        String portalCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_CODE);
        String categoryCodeList = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CATEGORY_CODE_EQ);
        String verifiedPortalCode = vendorService.checkGameCodeIsOpenEQGame(categoryCodeList, gameSession.getVendorGameCode(), portalCodeEQ, portalCode);

        ValidationUtils.isEquals(userName, authenticateDto.getUserName());
        ValidationUtils.isEquals(password, authenticateDto.getPassword());
        ValidationUtils.isEquals(verifiedPortalCode, authenticateDto.getPortalCode());

        if (authenticateDto.getDefenceCode() != null) {
            //Verify Idempotent Defence Code
            ValidationUtils.isEquals(gameSession.getVendorToken(), authenticateDto.getDefenceCode(), InvalidTokenException::new);

        }
    }

}
