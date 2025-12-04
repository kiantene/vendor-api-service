package com.nextgen.gameaggregator.vendor.ezugi.response;

import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import lombok.Data;

@Data
public class SuccessResponse {
    private final Integer errorCode = ResponseCodes.OK;
    private final String errorDescription = ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.OK);
    private Integer operatorId;
}
