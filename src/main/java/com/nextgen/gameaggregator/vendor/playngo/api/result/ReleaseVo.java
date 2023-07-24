package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.playngo.vo.CommonVo;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "release")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReleaseVo extends CommonVo {

    @JacksonXmlProperty(localName = "real")
    private String real;
    @JacksonXmlProperty(localName = "statusCode")
    private String statusCode;
    @JacksonXmlProperty(localName = "statusMessage")
    private String statusMessage;
}
