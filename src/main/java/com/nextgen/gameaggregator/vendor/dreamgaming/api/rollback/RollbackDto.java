package com.nextgen.gameaggregator.vendor.dreamgaming.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.dreamgaming.dto.DetailDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.dto.MemberDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto implements RollbackData {

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

    private MemberDto member;

    @Override
    public String getRollbackId() {
        return StringUtils.stripStart(this.ticketId, "0");
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return this.detailDto.getExt();
    }
}
