package com.nextgen.gameaggregator.vendor.cpgame.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataVo {
    private Double balance;

    private String currency;

    private Long updated_ms;
}
