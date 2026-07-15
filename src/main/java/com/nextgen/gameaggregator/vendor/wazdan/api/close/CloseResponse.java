package com.nextgen.gameaggregator.vendor.wazdan.api.close;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloseResponse {

    @NotNull
    private Integer status;
}
