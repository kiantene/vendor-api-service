package com.nextgen.gameaggregator.vendor.ambslot.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ambslot.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ambslot.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ambslot.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ambslot.service.VendorService;
import com.nextgen.gameaggregator.vendor.ambslot.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.DataVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.StatusVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.WalletVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RollbackAction {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorService vendorService;

    @Autowired
    public RollbackAction(HttpService httpService,
                          VendorLineService vendorLineService,
                          GameSessionService gameSessionService,
                          WalletService walletService,
                          ValidationService validationService,
                          VendorService vendorService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.ROLLBACK)
    public RollbackVo rollBack(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        RollbackDto rollbackDto = null;

        StatusVo statusVo = new StatusVo();
        BalanceVo balanceVo = new BalanceVo();
        DataVo dataVo = new DataVo();
        WalletVo walletVo = new WalletVo();
        RollbackVo rollbackVo = new RollbackVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // get x-ambslot-signature value for validation
            String signature = httpService.getHeadersInfo(request).get(EndPoints.HEADER_SIGNATURE);
            rollbackDto = HttpService.convertJsonToDto(body, RollbackDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(rollbackDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(rollbackDto.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollbackDto, gameSession, signature, body);

            // Retrieve the latest wallet balance from Operator
            WalletRequest walletRequest = walletService.processRollback(rollbackDto, gameSession, vendorService, httpRequestLog);
            BigDecimal balance = walletRequest.getBalanceAfter();
            BigDecimal beforeBetBalance = walletRequest.getBalanceBefore();

            statusVo.setCode(ResponseCodes.SUCCESS);
            statusVo.setMessage(ResponseCodes.SUCCESS_MSG);

            String dateTime = VendorService.convertUnixToDateTime(System.currentTimeMillis());

            walletVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            walletVo.setLastUpdate(dateTime);

            balanceVo.setBefore(beforeBetBalance);
            balanceVo.setAfter(balance.setScale(2, RoundingMode.DOWN));

            dataVo.setUsername(rollbackDto.getUsername());
            dataVo.setWallet(walletVo);
            dataVo.setBalance(balanceVo);
            dataVo.setRefId(rollbackDto.getTransactionId());

            rollbackVo.setData(dataVo);
        } catch (InvalidCredentialsException e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_AGENT);
            statusVo.setMessage(ResponseCodes.INVALID_AGENT_MSG);
        } catch (BetRefundIdempotentViolationException |
                 BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.TRANSACTION_CANCELED);
            statusVo.setMessage(ResponseCodes.TRANSACTION_CANCELED_MSG);
        } catch (InvalidRequestException |
                 JsonProcessingException |
                 BetNotFoundException |
                 InvalidSignatureException |
                 CredentialNotFoundException e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_REQUEST);
            statusVo.setMessage(ResponseCodes.INVALID_REQUEST_MSG);
        } catch (AuthenticationException |
                 InvalidPlayerException e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_TIMEOUT_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_TIMEOUT_ERROR_MSG);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_TIMEOUT_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_TIMEOUT_ERROR_MSG);
        } finally {
            rollbackVo.setStatus(statusVo);
            httpService.end(httpRequestLog, rollbackVo);
        }

        return rollbackVo;
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollbackDto dto, GameSession gameSession, String signature, String body) throws
            InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException,
            DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidRequestException, InvalidSignatureException,
            JsonProcessingException, InvalidCredentialsException {

        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUsername());

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), InvalidPlayerException::new);

        //Verify agent is same with credential
        String agent = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.prefix);
        ValidationUtils.isEquals(agent.toLowerCase(), dto.getAgent(), InvalidCredentialsException::new);

        // Verify header value
        String secret = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.secret);
        VendorService.validateSignature(signature, body, secret);
    }
}
