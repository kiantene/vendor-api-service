package com.nextgen.gameaggregator.vendor.ifg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data //child class will inherit
@JacksonXmlRootElement(localName = "server")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {

    @JacksonXmlProperty(isAttribute = true)
    private String session;

    @JacksonXmlProperty(isAttribute = true)
    private String time;

    @JacksonXmlProperty(localName = "enter")
    private Object enter;

    @JacksonXmlProperty(localName = "getbalance")
    private Object getbalance ;

    @JacksonXmlProperty(localName = "roundbet")
    private Object roundbet;

    @JacksonXmlProperty(localName = "roundwin")
    private Object roundwin;

    @JacksonXmlProperty(localName = "refund")
    private Object refund;
}
