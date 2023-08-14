package com.nextgen.gameaggregator.vendor.booongo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorVo {
    private String code;

    private Integer httpStatus;
}
