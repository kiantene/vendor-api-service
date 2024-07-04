package com.nextgen.gameaggregator.vendor.pinnacle.api.refund;

import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Formats;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class RefundDto extends ActionsWagerInfoDto implements SportRefundData {

    private String externalTransactionId;

    @Override
    public String getVendorBetId() {
        return this.getWagerId().toString();
    }

    @Override
    public String getRoundId() {
        if (isMultipleBet()) {
            return this.getWagerMasterId().toString();
        } else {
            return this.getWagerId().toString();
        }
    }

    @Override
    public Long getTimestamp() {
        return VendorService.convertDateTimeStringToTimestamp(this.getTransactionDate(), Formats.DATE_TIME_FORMAT_T_SEPARATOR, Formats.GMT_MINUS_FOUR);
    }

    private boolean isMultipleBet() {
        boolean isMultipleBet = false;
        if (Objects.nonNull(this.getWagerMasterId())) {
            isMultipleBet = !this.getWagerId().toString().equalsIgnoreCase(this.getWagerMasterId().toString());
        }
        return isMultipleBet;
    }

}
