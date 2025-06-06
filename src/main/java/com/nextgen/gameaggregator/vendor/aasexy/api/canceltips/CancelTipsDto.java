package com.nextgen.gameaggregator.vendor.aasexy.api.canceltips;

import com.nextgen.gameaggregator.vendor.aasexy.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CancelTipsDto extends GeneralDto {

    private List<CancelTipsTransactionsDto> txns;
}
