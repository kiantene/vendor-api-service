package com.nextgen.gameaggregator.vendor.yeebet.api.rollback;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.Instant;

@Data
public class RollbackDto implements RollbackData {
    @NotBlank
    private String appid;

    @NotBlank
    private String username;

    @NotBlank
    private String notifyid;

    @NotBlank
    private String amount;

    @NotBlank
    // must be either 1, 7 or 9
    @Pattern(regexp = "[179]")
    private String type;

    @NotBlank
    private String serialnumber;

    @NotBlank
    private String errmsg;

    @NotBlank
    private String sign;

    @Override
    public String getRollbackId() {
        return this.getSerialnumber();
    }

    @Override
    public Long getVendorSettledTime() {
        return Instant.now().getEpochSecond();
    }
}
