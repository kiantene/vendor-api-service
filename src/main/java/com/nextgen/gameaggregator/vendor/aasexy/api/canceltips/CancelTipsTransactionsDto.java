package com.nextgen.gameaggregator.vendor.aasexy.api.canceltips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aasexy.dto.TipsInfoDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelTipsTransactionsDto implements RollbackData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String platformTxId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 21)
    private String userId;

    //    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
//    @Size(min = 1, max = 20)
//    @JsonProperty("platform")
    private String platform;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 10)
    private String gameType;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String gameCode;

    @NotBlank
    @Size(min = 1, max = 50)
    private String gameName;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(max = 50)
    private String type;

    private TipsInfoDto tipinfo;

    @Override
    public String getRollbackId() {
        return this.getPlatformTxId();
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
