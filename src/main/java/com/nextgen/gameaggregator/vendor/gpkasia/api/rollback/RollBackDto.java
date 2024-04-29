package com.nextgen.gameaggregator.vendor.gpkasia.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.dto.ActionDto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollBackDto extends ActionDto implements RollbackData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String api_token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String user;

    @NotNull
    @Positive
    private Double money;

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

    @Override
    public String getRollbackId() {
        return this.dealid;
    }

    @Override
    public Long getVendorSettledTime() {
        return Long.parseLong(this.timestamp) * 1000;
    }
}
