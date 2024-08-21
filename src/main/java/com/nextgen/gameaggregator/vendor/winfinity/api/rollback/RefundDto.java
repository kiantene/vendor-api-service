package com.nextgen.gameaggregator.vendor.winfinity.api.rollback;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundDto implements RollbackData {
    @NotBlank
    @Size(max = 32)
    private String tid;

    @NotBlank
    @Size(max = 24)
    private String tbid;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String uid;

    @NotBlank
    @Size(max = 4)
    private String cur;

    @Size(max = 10)
    private String gtp;

    @NotBlank
    @Size(max = 32)
    private String sid;

    @Size(max = 32)
    private String msid;

    @NotBlank
    @Size(max = 32)
    private String gid;

    @NotNull
    @PositiveOrZero
    private BigDecimal sum;

    private Long timestamp;

    @Size(max = 32)
    private String refid;

    @Override
    public String getRollbackId() {
        return tid;
    }

    @Override
    public Long getVendorSettledTime() {
        return (timestamp != null) ? timestamp / 1000L : System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
