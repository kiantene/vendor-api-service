package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundDto implements RollbackData {

    @NotNull
    @JsonProperty("gamestatemode")
    public Integer gameStateMode;

    @NotBlank
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

    @JsonProperty("amount")
    public BigDecimal amount;

    @JsonProperty("bonusamount")
    public BigDecimal bonusAmount;

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

    @JsonProperty("isbonus")
    public Boolean isBonus;

    @JsonProperty("dtevent")
    public String dtEvent;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("initialdebittransferid")
    public String initialDebitTransferId;

    @JsonProperty("accounttransactiontype")
    public Integer accountTransactionType;

    @JsonProperty("gameinfeature")
    public Boolean gameInFeature;

    @JsonProperty("buyfeatureid")
    public Integer buyFeatureId;

    @JsonProperty("featureno")
    public Integer featureNo;

    @JsonProperty("lastbonusaction")
    public Boolean lastBonusAction;

    @Override
    public String getRollbackId() {
        return this.originalTransferId;
    }

    @Override
    public Long getVendorSettledTime() {
        //convert date time string to timestamp
        if(this.getDtEvent() != null){
            LocalDateTime localDateTime = LocalDateTime.parse(this.getDtEvent(), DateTimeFormatter.ISO_DATE_TIME);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
            long timestamp = zonedDateTime.toInstant().toEpochMilli();
            return timestamp;
        }else {
            return null;
        }
    }
}
