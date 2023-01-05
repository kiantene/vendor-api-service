package com.nextgen.gameaggregator.vendor.pgsoft.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseVo<T> extends CommonVo {
    private T data = null;
//    public ResponseVo() {
//        super();
//    }
}
