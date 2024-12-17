package com.nextgen.gameaggregator.vendor.poker365.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.poker365.constant.Credentials;
import com.nextgen.gameaggregator.vendor.poker365.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.poker365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.poker365.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelService {
    private final HttpService httpService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final VendorPlayerService vendorPlayerService;
    Integer vendorPlayerId;

    @Autowired
    public CancelService(HttpService httpService, GameSessionService gameSessionService,
                         AgentPlayerService agentPlayerService, VendorLineService vendorLineService,
                         VendorService vendorService,
                         WalletService walletService, VendorPlayerService vendorPlayerService) {
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.agentPlayerService = agentPlayerService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.vendorPlayerService = vendorPlayerService;
    }

    @PostMapping(path = EndPoints.CANCEL_BET)
    public CommonVo cancel(HttpRequestLog httpRequestLog, String traceId) throws JsonProcessingException {
        CommonVo commonVo = new CommonVo();
        BigDecimal balance;
        try {

            String body = httpRequestLog.getRequestBody();
            CancelDto cancelDto = HttpService.convertJsonToDto(body, CancelDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(cancelDto);


            this.vendorPlayerId = Integer.valueOf(cancelDto.getMessage().getUserId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(Long.valueOf(vendorPlayerId), null);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());


            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(cancelDto, gameSession);

            balance = walletService.processRollback(traceId, cancelDto, gameSession, vendorService, httpRequestLog);

            commonVo.setBalance(balance);
            commonVo.setStatus(ResponseCodes.SUCCESS_200.status);

        } catch (Exception e) {
            commonVo.setStatus(ResponseCodes.FAIL.status);
            commonVo.setMsg(ResponseCodes.FAIL.message);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(CancelDto cancelDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(cancelDto);
    }

    private void doVerification(CancelDto cancelDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException, DisabledAgentPlayerException, CredentialNotFoundException, InvalidVendorLineException, InvalidPlayerException, CredentialNotFoundException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // FindVendorLine
        VendorLine vendorLine = vendorLineService.getVendorLineById(gameSession.getVendorLineId());
        Integer vendorLineId = vendorLine.getId();
        String cert = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.CERT);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(cert, cancelDto.getKey(), InvalidPlayerException::new);

        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorPlayerId()), cancelDto.getMessage().getUserId(), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}
