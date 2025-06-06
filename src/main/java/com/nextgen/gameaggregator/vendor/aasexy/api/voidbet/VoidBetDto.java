package com.nextgen.gameaggregator.vendor.aasexy.api.voidbet;

import com.nextgen.gameaggregator.vendor.aasexy.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class VoidBetDto extends GeneralDto {

    private List<VoidBetTransactionsDto> txns;
}
