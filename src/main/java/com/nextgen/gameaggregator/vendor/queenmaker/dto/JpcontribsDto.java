package com.nextgen.gameaggregator.vendor.queenmaker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JpcontribsDto {

    private String jpexternalid;
    private String jpcur;
    private BigDecimal jprate;
    private BigDecimal jpamt;
    private BigDecimal jpcvtamt;
    private BigDecimal jpbal;

}
