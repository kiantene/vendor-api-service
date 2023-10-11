package com.nextgen.gameaggregator.vendor.ifg.vo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class ErrorVo {
    @JacksonXmlProperty(isAttribute = true)
    private String code;

    @JacksonXmlProperty(localName = "msg")
    private String msg;
}
