package com.nextgen.gameaggregator.vendor.habanero.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthVo implements HttpResponse {

    @JsonProperty("playerdetailresponse")
    private PlayerDetailResponseVo playerDetailResponseVo;

    public void setErrorResponseMessage(String responseMessage) {

        // Construct VO
        StatusVo statusVo = new StatusVo();
        this.playerDetailResponseVo.setStatusVo(statusVo);

        statusVo.setSuccess(false);
        statusVo.setMessage(responseMessage);
    }
    
    @Override
    public boolean hasError() {
        return false;
    }
}
