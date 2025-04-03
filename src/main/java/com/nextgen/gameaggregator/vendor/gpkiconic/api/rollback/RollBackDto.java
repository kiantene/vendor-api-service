package com.nextgen.gameaggregator.vendor.gpkiconic.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkiconic.dto.ActionDto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
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

    @Pattern(regexp = "true|false|1|0")
    @JsonProperty("istips")
    private String isTips;

    @Override
    public String getRollbackId() {
        return this.dealid;
    }

    @Override
    public Long getVendorSettledTime() {
        return Long.parseLong(this.timestamp) * 1000;
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
