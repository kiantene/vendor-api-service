package com.nextgen.gameaggregator.vendor.ag.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "TransferResponse")
public class ErrorVo {

    @JacksonXmlProperty(localName = "ResponseCode")
    @NotBlank
    private String result;

    @JsonIgnore
    private Integer httpStatus;
}
