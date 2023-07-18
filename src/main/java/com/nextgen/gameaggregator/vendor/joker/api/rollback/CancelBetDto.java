package com.nextgen.gameaggregator.vendor.joker.api.rollback;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelBetDto implements RollbackData {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String appid;

    @NotBlank(message = ResponseCodes.INVALID_SIGNATURE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String hash;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_:-]+$")
    private String id;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String betid;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 4, max = 32)
    private String username;

    @NotNull
    @Digits(integer = 13, fraction = 0)
    private Long timestamp;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String gamecode;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_:-]+$")
    private String roundid;

    @Override
    public String getRollbackId() {
        return String.valueOf(this.username + "_" + this.betid);
    }

    @Override
    public Long getVendorSettledTime() {
        return this.timestamp;
    }

}
