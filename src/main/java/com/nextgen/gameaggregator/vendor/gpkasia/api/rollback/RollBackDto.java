package com.nextgen.gameaggregator.vendor.gpkasia.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.PlatformType;
import com.nextgen.gameaggregator.vendor.gpkasia.dto.ActionDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollBackDto extends ActionDto implements RollbackData {
    @NotBlank
    @JsonProperty("api_token")
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String apiToken;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String user;

    @NotNull
    @PositiveOrZero
    private BigDecimal money;

    @NotBlank
    @Size(min = 10, max = 10)
    @Pattern(regexp = "\\d+")
    private String timestamp;

    @NotBlank
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String dealid;

    @NotBlank
    @Pattern(regexp = "[12]")
    private String code;

    @NotBlank
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String roundid;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String gameinfo;

    @NotBlank
    @Pattern(regexp = "\\d+")
    private String platform;

    @Pattern(regexp = "[01]")
    private String istips;

    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String root_roundid;

    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$") // not allow chinese word
    private String root_dealid;

    @Override
    public String getRollbackId() {
        String exTransId = this.dealid;

        //bgaming
        if (this.platform.equals(PlatformType.BGAMINGASIA) || this.platform.equals(PlatformType.BGAMINGLATAM)) {
            // dealid equals to null mean did not get any win amount in buy bonus game
            if (this.dealid == null) {
                exTransId = this.roundid;
            }
        }

        return exTransId;
    }

    @Override
    public Long getVendorSettledTime() {
        return Long.parseLong(this.timestamp) * 1000;
    }

    @Override
    public String getRoundId() {
        return this.roundid;
    }
}
