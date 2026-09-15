package com.nextgen.gameaggregator.vendor.esoterica.response;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class BetAndResultErrorResponse extends ErrorResponse {

    private CommonResponse bet;
    private CommonResponse win;
}
