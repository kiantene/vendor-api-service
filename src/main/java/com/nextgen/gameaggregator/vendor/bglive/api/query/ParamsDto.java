package com.nextgen.gameaggregator.vendor.bglive.api.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.bglive.dto.CommonParamsDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class ParamsDto extends CommonParamsDto {

    @JsonProperty("orders")
    private List<OrdersMapDto> ordersMapDto;
}
