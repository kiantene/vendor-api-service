package com.nextgen.gameaggregator.vendor.pragmaticplay.api.endround;

import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class EndRoundVo extends ResponseVo {
    private BigDecimal cash;        // Real balance of the player
    private BigDecimal bonus;       // Bonus balance of the player
}
