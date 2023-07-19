package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BonusSpinsDto {
    private Integer spins_count;
    private Integer bet_in_money;
}
