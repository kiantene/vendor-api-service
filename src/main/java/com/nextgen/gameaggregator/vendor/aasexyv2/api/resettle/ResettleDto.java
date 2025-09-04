package com.nextgen.gameaggregator.vendor.aasexyv2.api.resettle;

import com.nextgen.gameaggregator.vendor.aasexyv2.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResettleDto extends GeneralDto {

    private List<ResettleTransactionsDto> txns;
}
