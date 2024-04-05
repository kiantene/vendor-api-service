package com.nextgen.gameaggregator.vendor.bgaming.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto extends CommonDto implements RollbackData {
    private String betId;
    private String roundId;
    private Long timestamp;

    @Override
    public String getRollbackId() {
        return this.getVendorRoundId();
    }

    @Override
    public Long getVendorSettledTime() {
        return VendorService.getTimestamp();
    }
}
