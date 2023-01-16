package com.nextgen.gameaggregator.vendor.cq9.api.refund;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetRefundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RefundAction {
    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.REFUND, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseVo<CommonVo> refund(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();
        String wToken = request.getHeader("wtoken");

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        CommonVo commonVo = new CommonVo();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            RefundDto refundDto = HttpService.convertQueryStringToDto(body, RefundDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(refundDto, wToken);

            // 2. Gather require data
            BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionId(refundDto.getMtcode(), 3);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(betHistory.getGameSessionToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(refundDto, wToken, betHistory);

            // 5. Send refund to Operator
            BetRefundEvent betRefundEvent = walletService.processRefund(traceId, refundDto.getMtcode(), gameSession, body);

            commonVo.setBalance(betRefundEvent.getLastBalance());
            commonVo.setCurrency(gameSession.getCurrencyCode());

            responseVo.setData(commonVo);

        } catch (AuthenticationException authenticationException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (BetNotFoundException betNotFoundException) {
            statusVo.setCode(ResponseCodes.TRANSACTION_RECORD_NOT_FOUND);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (InvalidVendorLineException invalidVendorLineException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (RecordNotFoundException recordNotFoundException) {
            statusVo.setCode(ResponseCodes.TRANSACTION_RECORD_NOT_FOUND);

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(RefundDto refundDto, String wToken) throws InvalidRequestException {
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(refundDto);
    }

    private void doVerification(RefundDto refundDto, String wToken, BetHistory betHistory) throws InvalidVendorLineException, CredentialNotFoundException {
        // 3. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(betHistory.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 4. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);
    }
}
