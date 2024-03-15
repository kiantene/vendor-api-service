package com.nextgen.gameaggregator.vendor.saba.api.balance;

import com.nextgen.gameaggregator.vendor.saba.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetBalanceDto extends GeneralDto {

    private String userId;
}
