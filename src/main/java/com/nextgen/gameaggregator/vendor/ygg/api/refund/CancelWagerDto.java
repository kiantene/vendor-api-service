package com.nextgen.gameaggregator.vendor.ygg.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelWagerDto implements RollbackData {

    @NotBlank
    @Size(max = 255)
    private String org;

    @NotBlank
    @Size(max = 50)
    @JsonProperty("playerid")
    private String playerId;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String reference;


    @Override
    public String getRollbackId() {
        return this.reference;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

    @Override
    public String getRoundId() {
        return this.reference;
    }
}
