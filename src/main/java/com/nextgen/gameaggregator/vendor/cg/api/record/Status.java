package com.nextgen.gameaggregator.vendor.cg.api.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Status {
    String createtime;
    String endtime;
    String status;
    String message;
}
