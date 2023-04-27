package com.nextgen.gameaggregator.vendor.mg.api.updateBalance;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.vendor.mg.constant.DeviceType;
import com.nextgen.gameaggregator.vendor.mg.constant.EventType;
import com.nextgen.gameaggregator.vendor.mg.constant.TxnType;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateBalanceDto {
    @NotBlank
    @Size(max = 6)
    private TxnType txnType;

    @NotBlank
    @Size(max = 50)
    private EventType txnEventType;

    @NotBlank
    @Size(max = 50)
    private String playerId;
    
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotBlank
    @Size(max = 256)
    private String txnId;

    @NotBlank
    @Size(max = 50)
    private String contentCode;

    @Size(max = 256)
    private String betId;

    @Size(max = 256)
    private String roundId;

    private String metaData;

    @Size(max = 7)
    private DeviceType deviceType;

    private String platformType;

    @Min(value = 0)
    @Max(value = 1)
    private Integer completed;

    @Size(max = 50)
    private String channel;

    @NotNull
    private Long creationTimeMs;

    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_,~().!\\*'\\:@;-]*$")
    private String extOperatorToken;
}
