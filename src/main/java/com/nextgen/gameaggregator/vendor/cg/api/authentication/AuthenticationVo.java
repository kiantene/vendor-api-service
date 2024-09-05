package com.nextgen.gameaggregator.vendor.cg.api.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationVo implements HttpResponse {
    @JsonProperty("channelId")
    private String channelId;
    @JsonProperty("accountId")
    private String accountId;
    @JsonProperty("nickName")
    private String nickName;
    @JsonProperty("errorCode")
    private Integer errorCode;
    @JsonIgnore
    private String encrypt;

    @Override
    public boolean hasError() {
        return false;
    }
}
