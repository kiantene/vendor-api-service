package com.nextgen.gameaggregator.vendor.playngo.api.rollback;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.playngo.vo.CommonVo;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "cancelReserve")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelReserveVo extends CommonVo implements HttpResponse {

    @Size(max = 64)
    @JacksonXmlProperty(localName = "externalTransactionId")
    private String externalTransactionId;

}
