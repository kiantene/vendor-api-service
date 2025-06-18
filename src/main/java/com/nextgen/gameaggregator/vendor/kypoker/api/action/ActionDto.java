package com.nextgen.gameaggregator.vendor.kypoker.api.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {

    @NotNull
    private Integer s;

    private HttpRequestLog httpRequestLog;

}
