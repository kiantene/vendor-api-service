package com.nextgen.gameaggregator.vendor.habanero.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthVo implements HttpResponse {

    @JsonProperty("playerdetailresponse")
    private PlayerDetailResponseVo playerDetailResponseVo;

    public AuthVo() {
        this.setPlayerDetailResponseVo(new PlayerDetailResponseVo());
        this.setResponseCode(ResponseCodes.AUTH_SUCCESS);
    }

    public void setResponseCode(ResponseCodes responseCode) {
        this.getPlayerDetailResponseVo().getStatusVo().setSuccess(responseCode.success);
        this.getPlayerDetailResponseVo().getStatusVo().setAuthError(responseCode.authError);
        this.getPlayerDetailResponseVo().getStatusVo().setMessage(responseCode.message);
    }

    @Override
    public boolean hasError() {
        return false;
    }
}
