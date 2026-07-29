package com.nextgen.gameaggregator.vendor.mtlive.api.betandresult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetAndResultRequest {

    @NotBlank
    @Size(max = 10)
    private String system_code;

    @NotBlank
    @Size(max = 15)
    private String web_id;

    @NotBlank
    @Size(max = 20)
    private String user_id;

    @NotBlank
    private String tip_sn;

    @NotBlank
    private String game_code;

    @NotBlank
    private String game_name;

    @NotBlank
    private String table_code;

    @Digits(integer = 20, fraction = 2)
    @PositiveOrZero
    @NotNull
    private BigDecimal money;

    @NotBlank
    private String dealer_id;

    @NotBlank
    private String dealer_name;

    @NotBlank
    private String gift_id;

    @NotBlank
    private String gift_name;

    @NotBlank
    private String tran_time;

    @NotBlank
    private String currency;

    private String rid;

    @AssertTrue(message = "tip_sn must not contain spaces")
    public boolean isTipSnValid() {
        return tip_sn != null && !tip_sn.contains(" ");
    }

}
