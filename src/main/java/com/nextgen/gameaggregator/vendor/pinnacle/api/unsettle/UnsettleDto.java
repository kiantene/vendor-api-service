package com.nextgen.gameaggregator.vendor.pinnacle.api.unsettle;

import com.nextgen.gameaggregator.operator.sport.unsettle.SportUnsettleData;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Formats;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UnsettleDto extends ActionsWagerInfoDto implements SportUnsettleData {
    private String externalTransactionId;

    @Override
    public String getRoundId() {
        return this.getWagerId().toString();
    }

    @Override
    public Long getTimestamp() {
        return VendorService.convertDateTimeStringToTimestamp(this.getTransactionDate(), Formats.DATE_TIME_FORMAT_T_SEPARATOR, Formats.GMT_MINUS_FOUR);
    }
}
