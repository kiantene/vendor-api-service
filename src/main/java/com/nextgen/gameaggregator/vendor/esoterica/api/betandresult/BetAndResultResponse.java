package com.nextgen.gameaggregator.vendor.esoterica.api.betandresult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetAndResultResponse extends CommonResponse {

    private CommonResponse bet;
    private CommonResponse win;
}
