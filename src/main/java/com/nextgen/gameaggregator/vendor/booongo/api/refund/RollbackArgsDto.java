package com.nextgen.gameaggregator.vendor.booongo.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.booongo.dto.PlayerDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackArgsDto {
    @NotBlank
    private String transaction_uid;

    @Pattern(regexp = "^\\d+(\\.\\d+)?$")
    private String bet;

    @Pattern(regexp = "^\\d+(\\.\\d+)?$")
    private String win;

    @NotNull
    private Boolean round_started;

    @NotNull
    private Boolean round_finished;

    @NotNull
    private BigInteger round_id;

    @NotNull
    private PlayerDto player;

//    private Object bonus;

    private String tag;
}
