package com.nextgen.gameaggregator.vendor.evoplay.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {
    private String status;
    private ResponseDataVo data;
    private ResponseDataVo error;

    @JsonIgnore
    private ResponseCodes responseCode;

    public ResponseVo() {
        this.setResponseCode(ResponseCodes.SUCCESS);
    }

    public void setResponseCode(ResponseCodes responseCode) {
        this.responseCode = responseCode;
        this.status = responseCode.status;
        if (!this.responseCode.equals(ResponseCodes.SUCCESS)) {
            ResponseDataVo errorDataVo = new ResponseDataVo();
            errorDataVo.setNo_refund(Formats.NO_RESEND_CALLBACK);
            errorDataVo.setScope(Formats.SCOPE_INTERNAL);
            errorDataVo.setMessage(responseCode.message);
            this.setError(errorDataVo);
        }
    }

    @Override
    public boolean hasError() {
        return !this.responseCode.equals(ResponseCodes.SUCCESS);
    }
}