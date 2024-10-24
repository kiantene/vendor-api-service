package com.nextgen.gameaggregator.vendor.aviatrix.api.playerinfo;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.ResponseCodes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@RestController
@RequestMapping(EndPoints.PATH)
public class PlayerInfoAction {

    private final HttpService httpService;
    private final WalletService walletService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;

    @Autowired
    public PlayerInfoAction(HttpService httpService,
                            WalletService walletService,
                            GameSessionService gameSessionService,
                            VendorLineService vendorLineService,
                            AgentPlayerService agentPlayerService,
                            VendorGameService vendorGameService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
    }

    @PostMapping(EndPoints.PLAYER_INFO)
    public ResponseEntity<PlayerInfoVo> playerInfo(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        PlayerInfoVo responseVo = new PlayerInfoVo();
        BigInteger balance;
        try {
            PlayerInfoDto dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), PlayerInfoDto.class);

            //validation
            this.doValidation(dto);

            //verify token send from vendor
            GameSession gameSession = gameSessionService.verifyToken(dto.getSessionToken());

            //verify value
            this.doVerification(dto, gameSession);

            //balance check
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog).setScale(2, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).toBigInteger();
            //mapping
            responseVo.setPlayerId(gameSession.getVendorPlayerUsername());
            responseVo.setBalance(balance);
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());

        } catch (AuthenticationException authenticationException) {
            responseVo.setMessage(ResponseCodes.INVALID_SESSION_TOKEN);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, authenticationException);
        } catch (InvalidRequestException | InvalidFormatException invalidRequestException) {
            responseVo.setMessage(ResponseCodes.INVALID_REQUEST);
            responseVo.setHttpStatus(HttpStatus.BAD_REQUEST);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (TokenExpiredException tokenExpiredException) {
            responseVo.setMessage(ResponseCodes.SESSION_TOKEN_EXPIRED);
            responseVo.setHttpStatus(HttpStatus.UNAUTHORIZED);
            httpService.logError(httpRequestLog, tokenExpiredException);
        } catch (InvalidVendorLineException | DisabledVendorLineException invalidVendorLineException) {
            responseVo.setMessage(ResponseCodes.PLATFORM_NOT_FOUND);
            responseVo.setHttpStatus(HttpStatus.NOT_FOUND);
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (Exception e) {
            responseVo.setMessage(ResponseCodes.UNKNOWN_ERROR);
            responseVo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return new ResponseEntity<>(responseVo, responseVo.getHttpStatus());
    }

    private void doValidation(PlayerInfoDto dto) throws InvalidRequestException {
        //basic validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(PlayerInfoDto dto, GameSession gameSession) throws CredentialNotFoundException, InvalidVendorLineException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {
        String cid = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CID);
        ValidationUtils.isEquals(cid, dto.getCid(), InvalidVendorLineException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }

}
