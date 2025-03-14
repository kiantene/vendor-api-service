package com.nextgen.gameaggregator.vendor.aasexy.api.voidsettle;

import com.nextgen.gameaggregator.vendor.aasexy.dto.GeneralDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class VoidSettleDto extends GeneralDto {

    private List<VoidSettleTransactionsDto> txns;
}
