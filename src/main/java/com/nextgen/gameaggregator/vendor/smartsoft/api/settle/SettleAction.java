package com.nextgen.gameaggregator.vendor.smartsoft.api.settle;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
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

import javax.security.auth.login.CredentialException;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class SettleAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;

    public SettleAction(WalletService walletService,
                        HttpService httpService,
                        ValidationService validationService,
                        VendorService vendorService, VendorLineService vendorLineService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(path = EndPoints.WITHDRAW)
    public ResponseEntity<SettleVo> withdrawTransaction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        String method = httpRequestLog.getMethod();
        //Add request header log
        httpRequestLog.setRequestBody("Request Body: " + httpRequestLog.getRequestBody() + "\n Request Header: " + vendorService.getHeaders(request));
        SettleVo vo = new SettleVo();
        SettleDto settleDto;
        GameSession gameSession;
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.OK;

        try {
            settleDto = HttpService.convertJsonToDto(body, SettleDto.class);

            settleDto.setSignature(request.getHeader(Headers.REQUEST_SIGNATURE));
            settleDto.setSessionId(request.getHeader(Headers.SESSION_ID));
            settleDto.setUserName(request.getHeader(Headers.USER_NAME));
            settleDto.setClientExternalKey(request.getHeader(Headers.CLIENT_EXTERNAL_KEY));

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(settleDto);

            // Verify session
            gameSession = vendorService.checkGameSession(traceId, settleDto.getUserName());

            // Verify parameters (Verify against database values)
            this.doVerification(settleDto, gameSession, body, method);

            // Settle
            ResultType updatedResultType = vendorService.calculateResultType(settleDto.getBetAmount(), settleDto.getWinAmount(), settleDto.getJackpotAmount(), false);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleDto, updatedResultType, vendorService, httpRequestLog);

            vo.setTransactionId(httpRequestLog.getId());
            vo.setBalance(balance);

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            headers.add(Headers.ERROR_CODE, ResponseCode.LOSS_LIMIT.code.toString());
            headers.add(Headers.ERROR_MESSAGE, ResponseCode.LOSS_LIMIT.message);
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

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        ValidationUtils.validateRequest(dto.getTransactionInfoDto());
    }

    private void doVerification(SettleDto dto, GameSession gameSession, String body, String method) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, CredentialException, InvalidRequestException {

        //Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserName(), InvalidRequestException::new);

        //Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.signatureGenerator(secretKey, method, body), dto.getSignature(), AuthenticationException::new);

        //verify ClientExternalKey
        ValidationUtils.isEquals(gameSession.getVendorPlayerId().toString(), dto.getClientExternalKey(), AuthenticationException::new);
    }
}
