package com.nextgen.gameaggregator.vendor.aasexyv2.api.bet;

import com.nextgen.gameaggregator.vendor.aasexyv2.dto.GeneralDto;
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
