package com.nextgen.gameaggregator.vendor.mtlive.api.adjustment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdjustmentRequest {
    @NotBlank
    private String msg;

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

    @Digits(integer = 20, fraction = 2)
    @PositiveOrZero
    @NotNull
    private BigDecimal valid_money;

    @Digits(integer = 20, fraction = 2)
    @PositiveOrZero
    @NotNull
    private BigDecimal valid_money_result;

    @Digits(integer = 20, fraction = 2)
    @PositiveOrZero
    @NotNull
    private BigDecimal win_money;

    @Digits(integer = 20, fraction = 2)
    @NotNull
    private BigDecimal profit;

    @NotBlank
    private String settle_time;

    @NotBlank
    private String settle_date;

    @NotNull
    private Integer Status;

    private String rid;

    @AssertTrue(message = "bet_sn must not contain spaces")
    public boolean isBetSnValid() {
        return bet_sn != null && !bet_sn.contains(" ");
    }

    @AssertTrue(message = "Status must be 2, 3, or 4")
    public boolean isStatusValid() {
        return Status != null && (Status == 2 || Status == 3 || Status == 4);
    }
}
