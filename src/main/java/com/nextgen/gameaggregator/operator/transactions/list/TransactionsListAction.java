package com.nextgen.gameaggregator.operator.transactions.list;

import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "transactions/")
@Slf4j
public class TransactionsListAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private ValidationService validationService;

    @Autowired
    private TransactionListService transactionListService;

    @PostMapping(path = "list")
    public OperatorResponseVo<TransactionsListData> list(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<TransactionsListData> responseVo = new OperatorResponseVo<>();
        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            TransactionsListDto dto = HttpService.convertJsonToDto(body, TransactionsListDto.class);

            responseVo.setTraceId(dto.getTraceId());
            httpRequestLog.setTraceId(dto.getTraceId());
            log.info(dto.toString());

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(Endpoints.HEADER_API_KEY);
            //TODO (by Alex), check agent status
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);

            TransactionsListData transactionsListData =  transactionListService.getTransactionsList(dto, apiCredential.getId());
            responseVo.setData(transactionsListData);


        } catch (IllegalArgumentException illegalArgumentException) {
            // thrown when any field encountered type mismatch during conversion from json to dto
            log.error(illegalArgumentException.toString());
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

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
