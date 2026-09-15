package com.nextgen.gameaggregator.vendor.esoterica.api.betandresult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.esoterica.request.CommonRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetAndResultRequest {

    @Valid
    @NotNull
    private CommonRequest bet;

    @Valid
    @NotNull
    private CommonRequest win;
}
