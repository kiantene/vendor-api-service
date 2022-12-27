package com.nextgen.gameaggregator.vendor.pragmaticplay.api.result;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
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
public class ResultAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    ResultService resultService;

    @PostMapping(path = Endpoints.RESULT)
    public ResultVo betResult(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        ResultVo responseVo = new ResultVo();
        String traceId = UUID.randomUUID().toString();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            ResultDto dto = HttpService.convertQueryStringToDto(body, ResultDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);

            // 2. Verify session token
            GameSession session = gameSessionService.verifyToken(dto.getToken());

            // 3. Retrieve vendor line credentials and secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(session.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
            VendorService.validateHash(body, secretKey);

            // use the bet result round id to find the bet request info from log request
//            SeamlessBetHistoryRequest seamlessBetRequest = resultService.getSeamlessBetRequestId(dto);

//            if (seamlessBetRequest == null){
//                // if not finding bet request info, will create as "others" record for extra handling
//                resultService.createRawBetHistorySeamlessOthersCouchBase(requestBody, authenticatedUser.getVendorCurrencyCode());
//            } else {
//                // if found the bet request info, will create as success result record (kafka topic)
//                resultService.createRecordToKafkaBetHistoryTopic(seamlessBetRequest.getBetHistoryId(), authenticatedUser, requestBody);
//            }

            // no matter bet request is found or not, will still proceed to send to operator
            // Call bet request operator GRPC to get the balance of the player
//            BigDecimal balance = resultService.getBetRequestBalanceFromGRPC(dto, traceId, authenticatedUser, seamlessBetRequest);

            responseVo.setTransactionId(traceId.replace("-", ""));
            responseVo.setCurrency("CNY"); // TODO: vendor currency code
            responseVo.setCash(new BigDecimal("1000"));
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());

//        } catch (AuthenticationException authenticationException) {
//            responseVo.setError(ResponseCodes.AUTHENTICATION_ERROR);

//        } catch (UnableToFindCredentialsException unableToFindCredentialsException) {
//            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);

//        } catch (InvalidSignatureException invalidSignatureException) {
//            responseVo.setError(ResponseCodes.INVALID_HASH);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpRequestLog.setErrorMessage(HttpService.getStackTrace(exception));

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            if (!responseVo.getError().equals(ResponseCodes.SUCCESS)) {
                httpRequestLog.setStatus(1);
            }
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }

        return responseVo;
    }
}
