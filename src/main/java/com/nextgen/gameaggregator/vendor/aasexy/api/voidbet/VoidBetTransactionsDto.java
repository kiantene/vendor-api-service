package com.nextgen.gameaggregator.vendor.aasexy.api.voidbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.aasexy.dto.GameInfoDto;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoidBetTransactionsDto implements RollbackData {
    @NotBlank
    @Size(max = 255)
    private String platformTxId;

    @NotBlank
    @Size(max = 50)
    private String userId;

    private String platform;

    private String gameType;

    @NotBlank
    @Size(max = 255)
    private String gameCode;

    private String gameName;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal betAmount;

    private String updateTime;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("roundId")
    private String roundId;

    private GameInfoDto gameInfo;

    private Integer voidType;

    @Override
    public String getRollbackId() {
        return this.getPlatformTxId();
    }

    @Override
    public Long getVendorSettledTime() {
        return VendorService.getTimeStamp(this.updateTime);
    }
}
