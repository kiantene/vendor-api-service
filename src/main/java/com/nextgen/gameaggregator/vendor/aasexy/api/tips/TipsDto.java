package com.nextgen.gameaggregator.vendor.aasexy.api.tips;

import com.nextgen.gameaggregator.vendor.aasexy.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TipsDto extends GeneralDto {

    private List<TipsTransactionsDto> txns;
}
