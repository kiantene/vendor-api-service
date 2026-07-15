package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.vendor.habanero.api.transfer.BonusDetailDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HabaneroBonusPayoutRequest {
    private FundTransferRequestDto fundTransferRequest;
    private FundInfoDto fundInfo;
    private BonusDetailDto bonusDetails;
    private String vendorGameCode;
}
