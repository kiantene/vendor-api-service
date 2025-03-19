package com.nextgen.gameaggregator.vendor.smartsoft.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateVo implements HttpResponse {
    @JsonProperty("SessionId")
    private String sessionId;

    @JsonProperty("UserName")
    private String userName;

    @JsonProperty("ClientExternalKey")
    private String clientExternalKey;

    @JsonProperty("CurrencyCode")
    private String currencyCode;

    @JsonProperty("PortalName")
    private String portalName;

    @Override
    public boolean hasError() {
        return false;
    }
}