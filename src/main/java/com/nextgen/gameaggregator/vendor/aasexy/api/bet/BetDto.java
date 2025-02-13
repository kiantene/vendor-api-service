package com.nextgen.gameaggregator.vendor.aasexy.api.bet;

import com.nextgen.gameaggregator.vendor.aasexy.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BetDto extends GeneralDto {

    @NotEmpty
    private List<BetTransactionsDto> txns;
}
