package com.nextgen.gameaggregator.vendor.smartsoft.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.smartsoft.vo.ResponseVo;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateVo extends ResponseVo {
    private String sessionId;
    private String userName;
    private String clientExternalKey;
    private String currencyCode;
    private String portalName;

}