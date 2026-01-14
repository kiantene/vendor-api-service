package com.nextgen.gameaggregator.vendor.dblive.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.dblive.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    private Integer code;
    private String message;
    private String data;
    private String signature;

    public ResponseVo() {
        this.setResponseCode(ResponseCodes.SUCCESS);
    }

    public <T> void setResponseSuccess(T requestObject, String signature) throws JsonProcessingException {
        this.code = ResponseCodes.SUCCESS.code;
        this.message = ResponseCodes.SUCCESS.description;
        ObjectMapper mapper = new ObjectMapper();
        this.data = mapper.writeValueAsString(requestObject);
        this.signature = signature;
    }

    public void setResponseCode(ResponseCodes responseCode) {
        this.code = responseCode.code;
        this.message = responseCode.description;
    }

    @Override
    public boolean hasError() {
        return !this.code.equals(ResponseCodes.SUCCESS.code);
    }
}
