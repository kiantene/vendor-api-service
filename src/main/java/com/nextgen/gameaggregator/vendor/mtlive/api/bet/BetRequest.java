package com.nextgen.gameaggregator.vendor.mtlive.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRequest {

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
    @Size(max = 15)
    private String bet_sn;

    @NotBlank
    private String game_code;

    @NotBlank
    private String game_name;

    @NotBlank
    private String table_code;

    @NotBlank
    private String play_code;

    @NotBlank
    private String play_name;

    @NotBlank
    private String odds;

    @Digits(integer = 20, fraction = 2)
    @PositiveOrZero
    @NotNull
    private BigDecimal order_money;

    private BigDecimal prepayment_money;

    @NotBlank
    private String order_time;

    @NotBlank
    private String settle_date;

    @NotBlank
    private String currency;

    @NotBlank
    private String ip;

    private String rid;

    @AssertTrue(message = "bet_sn must not contain spaces")
    public boolean isBetSnValid() {
        return bet_sn != null && !bet_sn.contains(" ");
    }

}
