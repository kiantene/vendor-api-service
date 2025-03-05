package com.nextgen.gameaggregator.vendor.wmlive.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.wmlive.api.action.GeneralActionDto;
import com.nextgen.gameaggregator.vendor.wmlive.constant.Formats;
import com.nextgen.gameaggregator.vendor.wmlive.service.VendorService;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeoutBetReturnDto extends GeneralActionDto implements RollbackData {


    @Override
    public String getRollbackId() {
        return this.getDealid();
    }

    @Override
    public Long getVendorSettledTime() {
        return VendorService.convertDateTimeStringToTimestamp(this.getRequestDate(), Formats.DATE_TIME_FORMAT);
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
