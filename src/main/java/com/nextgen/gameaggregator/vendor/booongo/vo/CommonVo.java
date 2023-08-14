package com.nextgen.gameaggregator.vendor.booongo.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {
    @JsonProperty("uid")
    private String uid;

    private ErrorVo error;

    @Override
    public boolean hasError() {
        return !(this.error == null);
    }
}
