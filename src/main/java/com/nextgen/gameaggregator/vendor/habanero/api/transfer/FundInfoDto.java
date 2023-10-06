package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.service.CustomBooleanDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundInfoDto {

    @NotNull
    @JsonProperty("gamestatemode")
    public Integer gameStateMode;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("originaltransferid")
    public String originalTransferId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("transferid")
    public String transferId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("currencycode")
    public String currencyCode;

    @NotNull
    @JsonProperty("amount")
    public BigDecimal amount;

    @JsonProperty("bonusamount")
    public BigDecimal bonusAmount;

    @NotNull
    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    @JsonProperty("jpwin")
    public Boolean jpWin;

    @JsonProperty("jpid")
    public String jpId;

    @JsonProperty("jpname")
    public String jpName;

    @JsonProperty("jptypeid")
    public Integer jpTypeId;

    @JsonProperty("jpseed")
    public BigDecimal jpSeed;

    @JsonProperty("jpwinbase")
    public BigDecimal jpWinBase;

    @JsonProperty("jpcont")
    public BigDecimal jpCont;

    @NotNull
    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    @JsonProperty("isbonus")
    public Boolean isBonus;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9:._-]+$")
    @JsonProperty("dtevent")
    public String dtEvent;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("initialdebittransferid")
    public String initialDebitTransferId;

    @JsonProperty("accounttransactiontype")
    public Integer accountTransactionType;

    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    @JsonProperty("gameinfeature")
    public Boolean gameInFeature;

    @JsonProperty("buyfeatureid")
    public Integer buyFeatureId;

    @JsonProperty("featureno")
    public Integer featureNo;

    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    @JsonProperty("lastbonusaction")
    public Boolean lastBonusAction;
}
