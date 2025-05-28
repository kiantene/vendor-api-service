package com.nextgen.gameaggregator.vendor.aasexyv2.api.rollback;

import com.nextgen.gameaggregator.vendor.aasexyv2.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CancelDto extends GeneralDto {

    private List<CancelTransactionsDto> txns;
}
