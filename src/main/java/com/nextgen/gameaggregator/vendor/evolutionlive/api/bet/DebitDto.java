package com.nextgen.gameaggregator.vendor.evolutionlive.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evolutionlive.dto.DebitCreditCancelDto;
import com.nextgen.gameaggregator.vendor.evolutionlive.service.VendorService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitDto extends DebitCreditCancelDto implements BetResultData {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 250)
    private String sid; // Player session token

    @Override
    public String getExternalTransactionId() {
        return this.getTransaction().getRefId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTransaction().getRefId();
    } // id or refId

    @Override
    public String getRoundId() {
        // Vendor BackOffice only use front ID
        // e.g. (1766426e099ddd0a3aa82cba-rcj5y4fzmrmqaqtj) only ID before "-" needed
//        return this.getGame().getId().split("-")[0];
        return this.getTransaction().getRefId();
    }

    @Override
    public String getGameId() {
        return this.getGame().getDetails().getTable().getId();
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.getTransaction().getAmount();
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
        return getBetAmount();
    }

    @Override
    public Long getVendorBetTime() {
        return VendorService.getTimestamp();
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
        return BigDecimal.ZERO;
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
