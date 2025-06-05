package com.nextgen.gameaggregator.vendor.tbp.api.cancel;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.tbp.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.tbp.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.tbp.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class CancelAction {
    private final AgentPlayerService agentPlayerService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public CancelAction(AgentPlayerService agentPlayerService,
                        WalletService walletService,
                        HttpService httpService,
                        VendorLineService vendorLineService,
                        VendorService vendorService,
                        RequestIdempotentLogService requestIdempotentLogService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.CANCEL)
    public CancelVo rollbackAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        CancelVo responseVo = new CancelVo();
        CancelDto dto = new CancelDto();
        BigDecimal balance;
        GameSession gameSession;
        boolean isRequestExists = false;

        try {
            dto = HttpService.convertJsonToDto(body, CancelDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Request idempotent checking.
            if (requestIdempotentLogService.checkExists(dto, dto.getPlayerId()) == null) {
                requestIdempotentLogService.create(dto, dto.getPlayerId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // Get GameSession with username
            gameSession = vendorService.checkGameSession(traceId, dto.getPlayerId(), dto.getGameNumber(), dto.getSessionId());

            // Verify parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            //Rollback
            balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);

            responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            responseVo.setCasinoTransferId(dto.getCasinoTransferId());
            responseVo.setError(ResponseCode.OK);

        } catch (Exception e) {
            this.handleException(e, responseVo, dto, httpRequestLog);

        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getUsername());
            }
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(CancelDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        if (dto.getPlatformType() != null && dto.getPlatformType().trim().isEmpty()) {
            throw new InvalidRequestException("PlatformType field is Mandatory.");
        }
    }

    private void doVerification(CancelDto dto, GameSession gameSession) throws AuthenticationException, DisabledAgentPlayerException, DisabledVendorLineException, CredentialNotFoundException, GameNotSupportedException {
        //1. check session gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameNumber(), GameNotSupportedException::new);

        //2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        //3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        //4. verify Username, Password, PlayerId, Currency, SessionId
        vendorService.validate(dto.getUsername(), dto.getPassword(), dto.getPlayerId(), dto.getCurrency(), dto.getSessionId(), gameSession);

        //5. Verify GameNumber
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameNumber(), AuthenticationException::new);

    }

    @ExceptionHandler({
            BetResultIdempotentViolationException.class,
            InsufficientBalanceException.class,
            InvalidRequestException.class,
            AuthenticationException.class,
            Exception.class
    })
    private void handleException(Exception e, CancelVo responseVo, CancelDto dto, HttpRequestLog httpRequestLog) {

        if (e instanceof BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setBalance(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN));
            responseVo.setCasinoTransferId(dto.getCasinoTransferId());
            responseVo.setError(ResponseCode.OK);
        } else {
            responseVo.setError(ResponseCode.INTERNAL_SERVER_ERROR);
        }

        httpService.logError(httpRequestLog, e);
    }
}
