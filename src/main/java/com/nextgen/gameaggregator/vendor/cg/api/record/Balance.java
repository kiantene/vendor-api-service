package com.nextgen.gameaggregator.vendor.cg.api.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Balance {
    BigDecimal before;
    BigDecimal after;
}
