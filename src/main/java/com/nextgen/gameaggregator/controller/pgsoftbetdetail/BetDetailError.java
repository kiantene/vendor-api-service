package com.nextgen.gameaggregator.controller.pgsoftbetdetail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDetailError {
    private Integer code;
    private String message;
    private String traceId;
}
