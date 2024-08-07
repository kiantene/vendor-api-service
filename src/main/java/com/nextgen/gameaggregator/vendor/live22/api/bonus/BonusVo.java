package com.nextgen.gameaggregator.vendor.live22.api.bonus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.live22.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BonusVo extends ResponseVo {
    @JsonProperty("OldBalance")
    private BigDecimal oldBalance;
    @JsonProperty("NewBalance")
    private BigDecimal newBalance;
}

