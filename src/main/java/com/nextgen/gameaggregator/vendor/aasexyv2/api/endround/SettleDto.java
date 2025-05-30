package com.nextgen.gameaggregator.vendor.aasexyv2.api.endround;

import com.nextgen.gameaggregator.vendor.aasexyv2.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SettleDto extends GeneralDto {

    private List<SettleTransactionsDto> txns;
}
