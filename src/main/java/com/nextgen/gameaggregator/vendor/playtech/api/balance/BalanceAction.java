package com.nextgen.gameaggregator.vendor.playtech.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playtech.constant.Credentials;
import com.nextgen.gameaggregator.vendor.playtech.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.playtech.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.playtech.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.playtech.service.VendorService;
import com.nextgen.gameaggregator.vendor.playtech.vo.CommonBalanceVo;
import com.nextgen.gameaggregator.vendor.playtech.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.playtech.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BalanceAction {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final WalletService walletService;

    @Autowired
    public BalanceAction(HttpService httpService,
                         WalletService walletService,
                         VendorService vendorService,
                         GameSessionService gameSessionService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService) {
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public CommonVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        BalanceVo balanceVo = new BalanceVo();
        CommonBalanceVo commonBalanceVo = new CommonBalanceVo();
        CommonDto commonDto = new CommonDto();
        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            commonDto = HttpService.convertJsonToDto(body, CommonDto.class);
            // 2. Validate request parameters (Non-database calls)
            this.doValidation(commonDto);

            String removedPrefix = vendorService.getExtractToken(commonDto.getExternalToken());
            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(removedPrefix);

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, gameSession);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal getWalletBalance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 6. Set response data
            commonBalanceVo.setReal(getWalletBalance.setScale(2, RoundingMode.DOWN));
            commonBalanceVo.setTimestamp(VendorService.returnTime());
            balanceVo.setBalance(commonBalanceVo);

        } catch (InvalidPlayerException e) {
            balanceVo.setError(ErrorVo.from(ResponseCodes.ERR_PLAYER_NOT_FOUND));
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            balanceVo.setError(ErrorVo.from(ResponseCodes.ERR_AUTHENTICATION_FAILED));
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            balanceVo.setError(ErrorVo.from(ResponseCodes.ERR_REGULATORY_GENERAL));
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            balanceVo.setError(ErrorVo.from(ResponseCodes.INTERNAL_ERROR));
            httpService.logError(httpRequestLog, e);
        } finally {
            balanceVo.setRequestId(commonDto.getRequestId());
            httpService.end(httpRequestLog, balanceVo);
        }

        return balanceVo;
    }

    private void doValidation(CommonDto commonDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonDto);
    }

    private void doVerification(CommonDto commonDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            CredentialNotFoundException,
            InvalidPlayerException {

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());
        // FindVendorLine
        String kioskPrefix = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.KIOSK_PREFIX);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(kioskPrefix + "_" + gameSession.getVendorPlayerUsername(),
                commonDto.getUserName(), InvalidPlayerException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}