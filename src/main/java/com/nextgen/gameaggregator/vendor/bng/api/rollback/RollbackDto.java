package com.nextgen.gameaggregator.vendor.bng.api.rollback;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;

@Data
public class RollbackDto implements RollbackData {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_\\-.]*$")
    @Size(max = 35)
    private String uid;

    @NotBlank
    private String token;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_\\-.]*$")
    @Size(min = 32)
    private String session;

    @NotBlank
    private String game_id;

    @NotBlank
    private String game_name;

    @NotBlank
    private String provider_id;

    @NotBlank
    private String provider_name;

    @NotBlank
    private String c_at;

    @NotBlank
    private String sent_at;

    @NotNull
    private RollbackArgsDto args;

    @Override
    public String getRollbackId() {
        return this.getArgs().getTransaction_uid();
    }

    @Override
    public Long getVendorSettledTime() {
        // Convert vendor time into our timestamp
        return getTimeStamp(this.getC_at());
    }

    public Long getTimeStamp(String datetime) {
        Instant instant = Instant.parse(datetime);
        return instant.toEpochMilli();
    }
}
