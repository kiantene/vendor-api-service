package com.nextgen.gameaggregator.vendor.booongo.api.rollback;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigInteger;
import com.nextgen.gameaggregator.vendor.booongo.dto.PlayerDto;

@Data
public class RollbackArgsDto {
    @NotBlank
    private String transaction_uid;

    private String bet;
    private String win;

    @NotNull
    private Boolean round_started;

    @NotNull
    private Boolean round_finished;

    @NotNull
    private BigInteger round_id;

    @NotNull
    private PlayerDto player;

    private Object bonus;
    private String tag;
}
