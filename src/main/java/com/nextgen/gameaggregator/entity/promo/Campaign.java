package com.nextgen.gameaggregator.entity.promo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Campaign {
    private String uuid;
    private Integer status;
}
