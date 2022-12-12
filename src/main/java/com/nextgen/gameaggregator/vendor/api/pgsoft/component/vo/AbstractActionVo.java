package com.nextgen.gameaggregator.vendor.api.pgsoft.component.vo;

import javax.annotation.Nullable;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.dto.AbstractActionDto;
import lombok.Data;

@Data
public class AbstractActionVo {

    // This variable will be null when there is no error.
    @Nullable
    private Integer error = null;

}