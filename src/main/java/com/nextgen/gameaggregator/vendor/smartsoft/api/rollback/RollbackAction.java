package com.nextgen.gameaggregator.vendor.smartsoft.api.rollback;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.Headers;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.smartsoft.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;

    public RollbackAction(WalletService walletService,
                          HttpService httpService,
                          VendorService vendorService,
                          VendorLineService vendorLineService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(path = EndPoints.ROLLBACK)
    public ResponseEntity<RollbackVo> rollbackTransaction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        String method = httpRequestLog.getMethod();
        //Add request header log
        httpRequestLog.setRequestBody("Request Body: \n" + httpRequestLog.getRequestBody() + "\nRequest Header: \n" + vendorService.getHeaders(request));
        RollbackDto rollbackDto;
        RollbackVo vo = new RollbackVo();
        GameSession gameSession;
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.OK;

        try {
            rollbackDto = HttpService.convertJsonToDto(body, RollbackDto.class);

            rollbackDto.setSignature(request.getHeader(Headers.REQUEST_SIGNATURE));
            rollbackDto.setSessionId(request.getHeader(Headers.SESSION_ID));
            rollbackDto.setUserName(request.getHeader(Headers.USER_NAME));
            rollbackDto.setClientExternalKey(request.getHeader(Headers.CLIENT_EXTERNAL_KEY));

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(rollbackDto);

            // Verify session
            gameSession = vendorService.checkGameSession(traceId, rollbackDto.getUserName(), rollbackDto.getRollbackTransactionInfoDto().getGameName());

            // Verify parameters (Verify against database values)
            this.doVerification(rollbackDto, gameSession, body, method);

            //Rollback
            BigDecimal balance = walletService.processRollback(traceId, rollbackDto, gameSession, vendorService, httpRequestLog);

            vo.setTransactionId(httpRequestLog.getId());
            vo.setBalance(balance);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setTransactionId(e.getTransactionId());
            vo.setBalance(e.getBalance());
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            headers.add(Headers.ERROR_CODE, ResponseCode.INTERNAL_ERROR.code.toString());
            headers.add(Headers.ERROR_MESSAGE, ResponseCode.INTERNAL_ERROR.message);
        } finally {
            httpService.end(httpRequestLog, vo);
        }
        return new ResponseEntity<>(vo, headers, status);
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        ValidationUtils.validateRequest(dto.getRollbackTransactionInfoDto());
    }

    private void doVerification(RollbackDto dto, GameSession gameSession, String body, String method) throws AuthenticationException, CredentialNotFoundException, InvalidRequestException {

        //Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserName(), InvalidRequestException::new);

        //Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.signatureGenerator(secretKey, method, body), dto.getSignature(), AuthenticationException::new);

        //verify ClientExternalKey
        ValidationUtils.isEquals(gameSession.getVendorPlayerId().toString(), dto.getClientExternalKey(), AuthenticationException::new);
    }
}