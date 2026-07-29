package com.nextgen.gameaggregator.vendor.cockfight6.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonSuccessResponse {
    private Integer code;
    private String msg;
    private BigDecimal balance;
    @JsonProperty("record_id")
    private Long recordId;

}
