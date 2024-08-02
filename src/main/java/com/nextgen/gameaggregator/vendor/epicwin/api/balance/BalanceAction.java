package com.nextgen.gameaggregator.vendor.epicwin.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.epicwin.constant.Credentials;
import com.nextgen.gameaggregator.vendor.epicwin.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.epicwin.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.epicwin.service.VendorService;
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

    @PostMapping(path = EndPoints.GET_BALANCE)
    public BalanceVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();
        BalanceDto dto = new BalanceDto();
        BalanceVo vo = new BalanceVo();

        try {
            String body = httpRequestLog.getRequestBody();
            dto = HttpService.convertJsonToDto(body, BalanceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Verify launch token
            GameSession gameSession = gameSessionService.verifyToken(dto.getAuthToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // Set response data
            vo.setBalance(balance.setScale(4, RoundingMode.DOWN));
            vo.setResponseDateTime(dto.getRequestDateTime());

        } catch (AuthenticationException e) {
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);
            vo.setResponseDateTime(dto.getRequestDateTime()); //set for vendor acceptance test
            vo.setBalance(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidSignatureException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_SIGNATURE);
            vo.setResponseDateTime(dto.getRequestDateTime()); //set for vendor acceptance test
            vo.setBalance(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidPlayerException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_PLAYER_PASSWORD);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException e) {
            if (e.getMessage() != null && e.getMessage().equals(String.valueOf(ResponseCodes.OPERATOR_ID_ERROR.Status))) {
                vo.setResponseCodes(ResponseCodes.OPERATOR_ID_ERROR); //check db credential (operatorId) with request body value of operatorId that sent from vendor
            } else {
                vo.setResponseCodes(ResponseCodes.INCOMING_REQUEST_INFO_INCOMPLETE);
            }
            httpService.logError(httpRequestLog, e);

        } catch (CredentialNotFoundException | CurrencyNotSupportedException e) {
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            InvalidRequestException,
            CredentialNotFoundException,
            InvalidSignatureException,
            CurrencyNotSupportedException {

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify OperatorId
        String operatorId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_ID);
        ValidationUtils.isEquals(operatorId, dto.getOperatorId(), () -> new InvalidRequestException(String.valueOf(ResponseCodes.OPERATOR_ID_ERROR.Status)));

        // Generate encryptString
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        String encryptString = "GetBalance" + dto.getRequestDateTime() + dto.getOperatorId() + secretKey + dto.getPlayerId();
        String toVerifySign = VendorService.generateSign(encryptString);

        // Verify signature
        VendorService.isSameSignature(dto.getSignature(), toVerifySign);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
