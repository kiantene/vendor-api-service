package com.nextgen.gameaggregator.vendor.cpgame.api.canceldebit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonDto;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto extends CommonDto implements RollbackData {

    @Override
    public String getRollbackId() {
        return super.getMessageDto().getBetId();
    }

    @Override
    public Long getVendorSettledTime() {
        return super.getTime() * 1000;
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
