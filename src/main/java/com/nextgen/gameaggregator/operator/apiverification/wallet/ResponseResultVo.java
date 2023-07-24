package com.nextgen.gameaggregator.operator.apiverification.wallet;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class ResponseResultVo<T> implements HttpResponse {
    private String apiUrl;
    private T requestHeaders;
    private T requestBody;
//    private T responseHeaders;
    private String responseBody;
    private Integer httpStatusCode;
    private Long requestStartTime;
    private Long requestEndTime;
    private String error;


    @Override
    public boolean hasError() {
        return false;
    }
}
