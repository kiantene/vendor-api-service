package com.nextgen.gameaggregator.vendor.pragmaticplay.api.refund;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetRefundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.*;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class RefundAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = Endpoints.REFUND)
    public ResponseVo refund(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        RefundVo responseVo = new RefundVo();
        String traceId = httpRequestLog.getTraceId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            RefundDto dto = HttpService.convertQueryStringToDto(body, RefundDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);
            ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
            ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);

            // 2. Retrieve vendor player information based on given userId
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(dto.getUserId());

            // 3. Retrieve vendor line's secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(vendorPlayer.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
            VendorService.verifyHash(body, secretKey);

            // 5. Send refund to Operator
            BetRefundEvent betRefundEvent = walletService.processRefund(traceId, dto.getExternalTransactionId(), vendorPlayer, body);

            // Emit event for additional asynchronous processing such as publishing data to a kafka topic
            EventDispatcherSystem.emitAsync(betRefundEvent);

            responseVo.setTransactionId(traceId);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                String validations = invalidRequestException.getValidation().toString();
                log.error(validations);
                httpRequestLog.setErrorMessage(validations);
            }

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setError(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setError(ResponseCodes.INVALID_HASH);

        } catch (BetNotFoundException betNotFoundException) {
            // Don't throw error even if Bet is not found
            responseVo.setTransactionId(traceId); // TODO: need to update to the correct refund Id
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }
}
