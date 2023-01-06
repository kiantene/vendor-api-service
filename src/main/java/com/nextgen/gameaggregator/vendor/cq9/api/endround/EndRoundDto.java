package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDto {
    @NotBlank
    @Size(min = 1, max = 36)
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
    private EndRoundDataDto data;
    @NotBlank
    private String createTime;
    private BigDecimal freegame;
    private BigDecimal bonus;
    private BigDecimal luckydraw;
    private BigDecimal jackpot;
    private List<BigDecimal> jackpotcontribution;
}
