package com.nextgen.gameaggregator.vendor.queenmaker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitTransactionsDto {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String userid;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String authtoken;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String brandcode;

    @NotNull
    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal amt;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 3, max = 8)
    private String cur;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 40)
    private String ipaddress; // optional

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 36)
    private String ptxid;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 36)
    private String refptxid;

    @NotNull
    @Range(min = 0)
    private Integer txtype;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 36)
    private String timestamp;

    @NotNull
    @Range(min = 0)
    private Integer platformtype;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String gpcode;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String gamecode;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String gamename;

    @NotNull
    @Digits(integer = 1, fraction = 0, message = "Value must be either 0 or 1")
    @Range(min = 0, max = 1)
    private Integer gametype;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String externalgameid;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 36)
    private String roundid;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 64)
    private String externalroundid;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 36)
    private String betid; // optional

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 64)
    private String externalbetid; // optional

    private String senton;

    private Boolean isclosinground;

    @NotNull
    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal ggr;

    @NotNull
    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal turnover;

    @NotNull
    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal unsettledbets;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 20)
    private String walletcode; // optional

    @NotNull
    private Integer bonustype; // optional

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String bonuscode; // optional

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 2000)
    private String desc; // optional

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 255)
    private String jpexternalid; // optional

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 3, max = 8)
    private String jpcur; // optional

    @Range(min = 0)
    @Digits(integer = 12, fraction = 3)
    private BigDecimal jprate;

    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal jpamt;

    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal jpcvtamt;

    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal jpbal;

    private List<JpcontribsDto> jpcontribs;

    @Range(min = 0)
    @Digits(integer = 12, fraction = 6)
    private BigDecimal commission; // optional

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 14)
    private String redeemcode; // optional

}
