package com.nextgen.gameaggregator.vendor.live22.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.live22.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ResponseVo implements HttpResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("Status")
    private Integer status;
    @JsonProperty("Description")
    private String description;
    @JsonProperty("ResponseDateTime")
    private String responseDateTime;

    @JsonIgnore
    private ResponseCodes responseCodes;

    public ResponseVo() {
        this.setResponseCodes(ResponseCodes.OK);
    }

    public void setResponseCodes(ResponseCodes responseCodes) {
        this.responseCodes = responseCodes;
        this.status = responseCodes.Status;
        this.description = responseCodes.Description;
    }

    @Override
    public boolean hasError() {
        return !this.responseCodes.equals(ResponseCodes.OK);
    }

}
