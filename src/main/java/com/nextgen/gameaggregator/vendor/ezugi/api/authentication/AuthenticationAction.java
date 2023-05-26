package com.nextgen.gameaggregator.vendor.ezugi.api.authentication;

import com.couchbase.client.core.deps.com.google.common.io.CharStreams;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nextgen.gameaggregator.vendor.ezugi.service.VendorService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthenticationAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @PostMapping(path = EndPoints.AUTHENTICATION)
    public CommonVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        // Construct Vo
        AuthenticationVo authenticationVo = new AuthenticationVo();
        try {
            String body = httpRequestLog.getRequestBody();
            AuthenticationDto authenticationDto = HttpService.convertJsonToDto(body, AuthenticationDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(authenticationDto);

            //Get GameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.verifyToken(authenticationDto.getToken());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(authenticationDto, gameSession, httpRequestLog, request);

            //Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            authenticationVo.setToken(authenticationDto.getToken());
            authenticationVo.setOperatorId(authenticationDto.getOperatorId());
            authenticationVo.setUid(gameSession.getVendorPlayerUsername());
            authenticationVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            authenticationVo.setCurrency(gameSession.getVendorCurrencyCode());
            authenticationVo.setErrorCode(ResponseCodes.COMPLETED_SUCCESSFULLY);
            authenticationVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(authenticationVo.getErrorCode()));
            authenticationVo.setTimestamp(System.currentTimeMillis());
        }catch (Exception e){
            httpService.logError(httpRequestLog, e);
        }finally {
            httpService.end(httpRequestLog, authenticationVo);
        }
        return authenticationVo;
    }
    private void doValidation(AuthenticationDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
    private void doVerification(AuthenticationDto dto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, NoSuchAlgorithmException, InvalidKeyException, CredentialNotFoundException, InvalidPlayerException, IOException, InvalidSignatureException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), dto.getToken(), AuthenticationException::new);

        //String hashKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.HASH_KEY);
        //VendorService.verifyHash(hashKey,httpRequestLog.getRequestBody(),request.getHeader("hash"));

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 5. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 6. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
