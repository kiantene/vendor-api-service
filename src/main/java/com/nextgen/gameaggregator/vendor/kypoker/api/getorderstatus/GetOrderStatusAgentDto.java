package com.nextgen.gameaggregator.vendor.kypoker.api.getorderstatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GetOrderStatusAgentDto {

    private String agent;

}
