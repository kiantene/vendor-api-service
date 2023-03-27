package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetNSettleDto {
    private String action;

    @NotBlank
    @Digits(integer = 13, fraction = 0)
    private Long ts;

    @NotNull
    private Long transferId;

    @NotNull
    private Long gameSeqNo;

    @Size(min = 1, max = 30)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;

    @NotNull
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @JsonProperty("gType")
    private Integer gType;

    @NotNull
    @JsonProperty("mType")
    private Integer mType;

    @NotNull
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String reportDate;

    @NotNull
    private String gameDate;

    @NotNull
    private String currency;

    @NotNull
    @Negative
    private BigDecimal bet;

    @NotNull
    @Positive
    private BigDecimal win;

    @NotNull
    private BigDecimal netWin;

    @NotNull
    private BigDecimal denom;

    @NotNull
    private String ipAddress;

    @NotNull
    private String clientType;

    @NotNull
    private Integer systemTakeWin;

    @NotNull
    private String lastModifyTime;

    private String sessionNo;

    @NotNull
    private BigDecimal mb;


    // Slot Only, gType = 0
    private BigDecimal jackpotWin;

    private BigDecimal jackpotContribute;

    @JsonProperty("hasFreegame")
    private Boolean hasFreeGame;


    // Fish Only, gType = 7
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String roomType;


    // Slot and Arcade, gType = 0 OR gType = 9
    private Boolean hasGamble;


    // Arcade and Lottery, gType = 9 OR gType = 12
    private Boolean hasBonusGame;
}
