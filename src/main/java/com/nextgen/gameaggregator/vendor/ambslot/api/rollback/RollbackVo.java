package com.nextgen.gameaggregator.vendor.ambslot.api.rollback;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.ambslot.vo.DataVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.ResponseVo;
import lombok.Data;

@Data
public class RollbackVo extends ResponseVo {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DataVo data;
}
