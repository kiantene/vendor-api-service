package com.nextgen.gameaggregator.core.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import lombok.Data;

@Data
public class ClientBalanceResponse {
//    @NotBlank(message = "UUID format only")
//    @Size(min = 36, max = 36, message = "UUID format only")
    private String traceId;
//    @NotNull(message = "status can not be blank")
    private ResponseCodes status;

    private String message;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private PlayerBalanceData data;
}
