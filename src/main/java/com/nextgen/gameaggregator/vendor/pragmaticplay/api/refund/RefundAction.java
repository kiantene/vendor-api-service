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
import java.math.BigDecimal;
import java.util.UUID;

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
        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        RefundVo responseVo = new RefundVo();
        String traceId = UUID.randomUUID().toString();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            RefundDto dto = HttpService.convertQueryStringToDto(body, RefundDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);
            ValidationUtils.validateVendorUsername(dto.getUserId());
            ValidationUtils.validateEquals(dto.getProviderId(), Credentials.PROVIDER_ID);

            // 2. Retrieve vendor player information based on given userId
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(dto.getUserId());

            // 3. Retrieve vendor line's secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(vendorPlayer.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
            VendorService.validateHash(body, secretKey);

            // 5. Send refund to Operator
            BetRefundEvent betRefundEvent = walletService.processRefund(traceId, dto.getExternalTransactionId(), vendorPlayer, body);

            // Emit event for additional asynchronous processing such as publishing data to a kafka topic
            EventDispatcherSystem.emitAsync(betRefundEvent);

            responseVo.setTransactionId(traceId);
            responseVo.setCurrency(betRefundEvent.getCurrency()); // TODO: vendor currency map
            responseVo.setCash(betRefundEvent.getBalance());
            responseVo.setBonus(BigDecimal.ZERO);

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
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            httpRequestLog.setErrorMessage(betNotFoundException.getMessage());

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpRequestLog.setErrorMessage(HttpService.getStackTrace(exception));

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            if (!responseVo.getError().equals(ResponseCodes.SUCCESS)) {
                httpRequestLog.setStatus(HttpService.ERROR);
            }
            httpRequestLog.setEndTime(System.currentTimeMillis());
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }

        return responseVo;
    }
}
