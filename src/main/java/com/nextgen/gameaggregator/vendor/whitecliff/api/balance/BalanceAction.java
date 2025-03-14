package com.nextgen.gameaggregator.vendor.whitecliff.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.Credentials;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseError;
import com.nextgen.gameaggregator.vendor.whitecliff.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.whitecliff.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(path = EndPoints.PATH)
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
                         WalletService walletService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    @PostMapping(EndPoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();

        CommonDto balanceDto;

        try {
            String body = URLDecoder.decode(httpRequestLog.getRequestBody(), StandardCharsets.UTF_8);

            //Define request body
            balanceDto = HttpService.convertJsonToDto(body, CommonDto.class);

            //Get header for Validation
            balanceDto.setSecretKey(request.getHeader("secret-key"));

            //using vendorPlayerId to find gameSession details
            GameSession gameSession = gameSessionService.verifyToken(balanceDto.getSid());

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN));

            responseVo.setStatus(ResponseCodes.SUCCESS);

        }catch (InvalidPlayerException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(ResponseError.INVALID_USER);
        }catch (InvalidSignatureException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.ACCESS_DENIED);
            httpService.logError(httpRequestLog, e);
        }catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(ResponseError.UNKNOWN_ERROR);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

    }

    private void doVerification(CommonDto dto, GameSession gameSession)
            throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException,
            CredentialNotFoundException, InvalidRequestException, InvalidPlayerException, InvalidSignatureException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
        
        // Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(secretKey, dto.getSecretKey(), InvalidSignatureException::new);

        //Verify UserId
        String vendorToken = String.valueOf(dto.getUserid());
        ValidationUtils.isEquals(vendorToken, gameSession.getVendorToken(), InvalidPlayerException::new);

        //Validate Prd_id
        String prdId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PRODUCT_ID);
        ValidationUtils.isEquals(String.valueOf(dto.getPrdId()), prdId);
    }
}
