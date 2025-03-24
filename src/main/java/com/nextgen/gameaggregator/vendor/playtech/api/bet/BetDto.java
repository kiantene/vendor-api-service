package com.nextgen.gameaggregator.vendor.playtech.api.bet;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.playtech.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.playtech.service.VendorService;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto extends CommonDto implements BetResultData {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameRoundCode")
    private String gameRoundCode;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transactionCode")
    private String transactionCode;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")
    @Size(max = 255)
    @JsonProperty("transactionDate")
    private String transactionDate;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JsonProperty("amount")
    private BigDecimal amount;


    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameCodeName")
    private String gameCodeName;

    @Override
    public String getExternalTransactionId() {
        return this.transactionCode;
    }

    @Override
    public String getVendorBetId() {
        return this.transactionCode;
    }

    @Override
    public String getRoundId() {
        return this.gameRoundCode;
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.convertStringToMillis(this.transactionDate);
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
