package com.nextgen.gameaggregator.vendor.dreamgaming.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.TransferType;
import com.nextgen.gameaggregator.vendor.dreamgaming.dto.DetailDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.dto.MemberDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto implements BetResultData {

    // MD5 token (agent + apiKey)
    @NotBlank
    @Pattern(regexp = "^[a-f0-9]{32}$")
    private String token;

    // Transfer serial number (unique identifier for the transfer)
    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[\\S]+$")
    private String data;

    // Ticket ID (multiple deduction records in one round)
    @NotBlank
    @Size(max = 255)
    private String ticketId;

    // Transfer type: 1: Bet, 2: Payout, 3: Append, 5: Gift, 6: Tip
    @NotNull
    private Integer type;

    // Bet detail (valid only for payout, containing all valid deduction records for the ticket)
    private String detail;

    private DetailDto detailDto;

    //Member
    private MemberDto member;

    @Override
    public String getExternalTransactionId() {
        return StringUtils.stripStart(this.ticketId, "0");
    }

    @Override
    public String getVendorBetId() {
        return StringUtils.stripStart(this.ticketId, "0");
    }

    @Override
    public String getRoundId() {
        return this.detailDto.getExt();
    }

    @Override
    public String getGameId() {
        return "";
    }

    @Override
    public BigDecimal getBetAmount() {
        if (this.type.equals(TransferType.BET)) {
            return this.getMember().getAmount().abs();
        }
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (this.type.equals(TransferType.PAYOUT) || this.type.equals(TransferType.GIFT) || this.type.equals(TransferType.TIP)) {
            return this.getMember().getAmount().abs();
        }
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
        return System.currentTimeMillis();
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
        if (this.getType().equals(TransferType.PAYOUT) || this.getType().equals(TransferType.GIFT) || this.type.equals(TransferType.TIP)) {
            return BetStatus.SETTLED;
        }
        return BetStatus.UNSETTLED;
    }
}