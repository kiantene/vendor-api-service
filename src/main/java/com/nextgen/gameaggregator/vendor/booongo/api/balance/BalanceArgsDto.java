package com.nextgen.gameaggregator.vendor.booongo.api.balance;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.booongo.dto.PlayerDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceArgsDto {
    @NotNull
    private PlayerDto player;

    private String tag;
}
