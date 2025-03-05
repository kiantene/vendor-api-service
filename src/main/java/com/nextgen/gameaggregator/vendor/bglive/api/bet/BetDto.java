package com.nextgen.gameaggregator.vendor.bglive.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.bglive.dto.CommonDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class BetDto extends CommonDto {

    @JsonProperty("params")
    private ParamsDto paramsDto;
    @JsonProperty("orders")
    private OrdersDto currentOrder;


}
