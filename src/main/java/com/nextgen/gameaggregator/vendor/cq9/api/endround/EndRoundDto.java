package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDto {
    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String account;
    @NotBlank
    @Size(min = 1, max = 36)
    private String gamehall;
    @NotBlank
    @Size(min = 1, max = 36)
    private String gamecode;
    @NotBlank
    @Size(min = 1, max = 30)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String roundid;
    @NotBlank
    private String data;
    @NotBlank
    private String createTime;
    @Positive
    @Digits(integer = 12, fraction = 10)
    private BigDecimal freegame;
    @Positive
    private BigDecimal bonus;
    @Positive
    private BigDecimal luckydraw;
    @Positive
    private BigDecimal jackpot;
    private List<@Positive @Digits(integer = 12, fraction = 10) BigDecimal> jackpotcontribution;
}
