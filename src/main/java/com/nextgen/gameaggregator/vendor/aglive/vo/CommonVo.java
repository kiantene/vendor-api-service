package com.nextgen.gameaggregator.vendor.aglive.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.vendor.aglive.constant.ResponseCodes;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "TransferResponse")
public class CommonVo implements HttpResponse {

    @JsonIgnore
    HttpHeaders headers;
    @JacksonXmlProperty(localName = "ResponseCode")
    @NotBlank
    private String result;
    @JacksonXmlProperty(localName = "Balance")
    private BigDecimal balance;
    @JsonIgnore
    private Integer httpStatus;
    @JsonIgnore
    private String xmlResponse;

    public CommonVo() {
        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_XML);
        this.headers.set("X-Integration-API-host", "api-1.operator.com");
    }

    public void setSuccessResponse(BigDecimal balance) {
        this.balance = balance;
        this.result = ResponseCodes.OK.message;
        this.httpStatus = ResponseCodes.OK.httpStatus;
    }

    public void setErrorResponse(ResponseCodes code) {
        this.result = code.message;
        this.httpStatus = code.httpStatus;
    }

    @Override
    public boolean hasError() {
        return !this.getResult().equals(ResponseCodes.OK.message);
    }
}

