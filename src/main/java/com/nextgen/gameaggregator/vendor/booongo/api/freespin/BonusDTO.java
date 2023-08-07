package com.nextgen.gameaggregator.vendor.booongo.api.freespin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BonusDTO {

    @NotBlank
    private String source;

    @NotBlank
    private String campaign;

    @NotBlank
    private String ext_bonus_id;

    @NotNull
    private BigInteger bonus_id;

    @NotBlank
    private String bonus_type;

    @NotBlank
    private String event;

    private String start_date;

    private String end_date;

    private String total_bet;

    private String total_win;

    private String played_bet;

    private String played_win;

    @NotNull
    private String status;
}
