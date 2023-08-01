package com.nextgen.gameaggregator.vendor.booongo.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto implements RollbackData {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(max = 35)
    private String uid;

    @NotBlank
    private String token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 32, max = 32)
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
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})$")
    private String c_at;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})$")
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
