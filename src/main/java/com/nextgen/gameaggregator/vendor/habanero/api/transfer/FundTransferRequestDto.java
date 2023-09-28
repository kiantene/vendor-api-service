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

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundTransferRequestDto {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("token")
    public String token;

    @JsonProperty("partnermeta")
    public String partnerMeta;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("accountid")
    public String accountId;

    @JsonProperty("customplayertype")
    public Integer customPlayerType;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("gameinstanceid")
    public String gameInstanceId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("friendlygameinstanceid")
    public String friendlyGameInstanceId;

    @NotNull
    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    @JsonProperty("isretry")
    public Boolean isRetry;

    @NotNull
    @JsonProperty("retrycount")
    public Integer retryCount;

    @NotNull
    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    @JsonProperty("isrefund")
    public Boolean isRefund;

    @NotNull
    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    @JsonProperty("isrecredit")
    public Boolean isRecredit;

    @JsonProperty("funds")
    public FundDto fundDto;

    @JsonProperty("gamedetails")
    public GameDetailDto gameDetailDto;

    @JsonProperty("bonusdetails")
    public BonusDetailDto bonusDetailDto;
}
