package com.nextgen.gameaggregator.vendor.booongo.api.balance;
import com.nextgen.gameaggregator.vendor.booongo.dto.PlayerDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BalanceArgsDto {
    @NotNull
    private PlayerDto player;

    private String tag;
}
