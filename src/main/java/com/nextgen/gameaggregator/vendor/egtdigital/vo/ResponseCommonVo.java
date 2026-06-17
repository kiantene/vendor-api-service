package com.nextgen.gameaggregator.vendor.egtdigital.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseCommonVo {

    private Long balance;

    private String statusCode;
}
