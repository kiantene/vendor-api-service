package com.nextgen.gameaggregator.operator.transactions.detail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "transaction/")
@Slf4j
public class TransactionDetailAction {

    @Autowired
    private HttpService httpService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private LanguageService languageService;

    @Autowired
    private BetHistoryService betHistoryService;

    @Autowired
    private VendorLineService vendorLineService;

    @Autowired
    private VendorService vendorService;

    @PostMapping(path = "detail")
    public OperatorResponseVo<TransactionDetailData> detail(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<TransactionDetailData> responseVo = new OperatorResponseVo<>();
        try {

            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            TransactionDetailDto dto = HttpService.convertJsonToDto(body, TransactionDetailDto.class);

            responseVo.setTraceId(dto.getTraceId());
            httpRequestLog.setId(dto.getTraceId());

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(EndPoints.HEADER_API_KEY);
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);

            // 3. Validate the signature
            String signature = request.getHeader(EndPoints.HEADER_SIGNATURE);
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);

            // 4. check if platform supported
            Language language = languageService.checkLanguageCode(dto.getDisplayLanguage());

            //5. check bet history detail
            IBetDetailUrlInfo iBetDetailUrlInfo = betHistoryService.getBetHistoryDetail(apiCredential.getAgent().getId(), dto.getBetId());

            //6. check vendor line
            VendorLine vendorLine = vendorLineService.getVendorLineById(iBetDetailUrlInfo.getVendorLineId(), iBetDetailUrlInfo.getVendorId());

            //7. check if vendor language supported
            VendorLanguageCode vendorLanguageCode = vendorService.findVendorLanguageCode(vendorLine.getVendor(), language);

            TransactionDetailData transactionDetailData = new TransactionDetailData();
            transactionDetailData.setBetDetail(iBetDetailUrlInfo);

            if (iBetDetailUrlInfo.getGameCategoryCode().equals("SPORT")) {
                transactionDetailData = betHistoryService.getSportBetDetail(iBetDetailUrlInfo, transactionDetailData, vendorLine, vendorLanguageCode);
            } else {
                transactionDetailData = betHistoryService.getDetailUrl(iBetDetailUrlInfo, transactionDetailData, vendorLine, vendorLanguageCode);
            }

            responseVo.setData(transactionDetailData);

        } catch (IllegalArgumentException illegalArgumentException) {
            log.error(illegalArgumentException.toString());
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

        } catch (JsonProcessingException jsonProcessingException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_REQUEST);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_TRANSACTION_NOT_EXISTS);

        } catch (InvalidVendorResponseException invalidVendorResponseException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_ERROR);

        } catch (InvalidVendorLineException invalidVendorLineException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_VENDOR);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_LINE_DISABLED);

        } catch (InvalidLanguageException invalidLanguageException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_LANGUAGE);

        } catch (VendorLanguageNotSupportedException vendorLanguageNotSupportedException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_VENDOR_LANGUAGE_NOT_SUPPORTED);

        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, exception);
            exception.printStackTrace();

        } finally {
            responseVo.setMessage(responseVo.getStatus().description);

        }
        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }
}
