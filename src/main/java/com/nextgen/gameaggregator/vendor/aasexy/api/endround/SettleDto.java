package com.nextgen.gameaggregator.vendor.aasexy.api.endround;

import com.nextgen.gameaggregator.vendor.aasexy.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SettleDto extends GeneralDto {

    private List<SettleTransactionsDto> txns;
}
