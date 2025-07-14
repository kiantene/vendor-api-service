package com.nextgen.gameaggregator.vendor.kypoker.api.cancel;

import com.nextgen.gameaggregator.vendor.kypoker.vo.ResponseObjectDto;
import lombok.Data;

@Data
public class CancelVo extends ResponseObjectDto {

    private Integer status;
    private Integer code;

}
