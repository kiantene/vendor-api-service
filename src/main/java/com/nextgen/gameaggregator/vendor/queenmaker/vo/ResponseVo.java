package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {

    @JsonIgnore
    protected ResponseCodes responseCodes;
    private Boolean dup;
    private Integer err;
    private String errdesc;

    public void setResponseCode(ResponseCodes responseCodes) {
        setResponseCode(responseCodes, "");
    }

    public void setResponseCode(ResponseCodes responseCodes, String errDesc) {
        this.responseCodes = responseCodes;
        this.setErr(responseCodes.err);
        this.setErrdesc(responseCodes.errdesc.replace(Formats.REPLACE_STRING, errDesc));
    }

    @Override
    public boolean hasError() {
        return this.responseCodes != null;
    }
}
