package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

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
    private String roundid;
    private String data;
    @NotBlank
    private String createTime;
    private BigDecimal freegame;
    private BigDecimal bonus;
    private BigDecimal luckydraw;
    private BigDecimal jackpot;
//    private List<BigDecimal> jackpotcontribution;

}
