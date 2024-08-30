package com.nextgen.gameaggregator.operator.transactions.list;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidFromTimeException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.exception.InvalidDateRangeException;


import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "transaction/")
@Slf4j
public class TransactionsListAction {
    public static final String REQUEST_TYPE = "TransactionList";
    private final HttpService httpService;
    private final ValidationService validationService;
    private final TransactionListService transactionListService;

    public TransactionsListAction(HttpService httpService,
                                  ValidationService validationService,
                                  TransactionListService transactionListService) {

        this.httpService = httpService;
        this.validationService = validationService;
        this.transactionListService = transactionListService;
    }

    @PostMapping(path = "list")
    public OperatorResponseVo<TransactionsListData> list(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestType(REQUEST_TYPE);
      //  httpRequestLog.setResponseLogged(false);
        OperatorResponseVo<TransactionsListData> responseVo = new OperatorResponseVo<>();

        OperatorResponseVo<TransactionsListData> responseVoLogging = new OperatorResponseVo<>();
        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            TransactionsListDto dto = HttpService.convertJsonToDto(body, TransactionsListDto.class);

            responseVo.setTraceId(dto.getTraceId());
            httpRequestLog.setId(dto.getTraceId());

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(EndPoints.HEADER_API_KEY);
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);
            httpRequestLog.setAgentId(apiCredential.getAgent().getId());

            // 3. Validate the signature
            String signature = request.getHeader(EndPoints.HEADER_SIGNATURE);
            validationService.validateSignature(body, apiCredential.getApiSecret(), signature);

            // 4. Validate from time not before last 60 days
            transactionListService.isStartTimeValid(dto.getFromTime());
            // 5. Validate date range not more than one day
            transactionListService.isDateRangeValid(dto.getFromTime(), dto.getToTime());

            if(dto.getPageSize()<2000){
                dto.setPageSize(2000);
            }

            TransactionsListData transactionsListData =  transactionListService.getTransactionsList(dto, apiCredential.getAgent().getId());
            responseVo.setData(transactionsListData);


            TransactionsListData transactionsListDataLog = new TransactionsListData();
            responseVoLogging.setTraceId(dto.getTraceId());
            transactionsListDataLog.setTotalItems(transactionsListData.getTotalItems());
            transactionsListDataLog.setCurrentPage(transactionsListData.getCurrentPage());
            transactionsListDataLog.setTotalPages(transactionsListData.getTotalPages());
            responseVoLogging.setData(transactionsListDataLog);

        } catch (IllegalArgumentException illegalArgumentException) {
            // thrown when any field encountered type mismatch during conversion from json to dto
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);
            httpService.logError(httpRequestLog, illegalArgumentException);

        } catch (JsonProcessingException jsonProcessingException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);
            httpService.logError(httpRequestLog, jsonProcessingException);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_AUTHENTICATION_FAILED);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_SIGNATURE);
            httpService.logError(httpRequestLog, invalidSignatureException);

        } catch (InvalidFromTimeException invalidFromTimeException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_FROM_TIME);
            httpService.logError(httpRequestLog, invalidFromTimeException);

        } catch (InvalidDateRangeException invalidDateRangeException) {
            responseVo.setResponseCode(ResponseCodes.Status.SC_INVALID_DATE_RANGE);
            httpService.logError(httpRequestLog, invalidDateRangeException);

        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            responseVo.setMessage(responseVo.getStatus().description);

            responseVoLogging.setMessage(responseVo.getStatus().description);

            httpService.end(httpRequestLog, responseVoLogging);
        }

        return responseVo;
    }
}
