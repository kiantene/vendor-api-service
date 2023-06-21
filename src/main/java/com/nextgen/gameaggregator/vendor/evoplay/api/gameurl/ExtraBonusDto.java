package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtraBonusDto {
    private BonusSpinsDto bonus_spins;
    private FreespinsOnStartDto freespins_on_start;
}
