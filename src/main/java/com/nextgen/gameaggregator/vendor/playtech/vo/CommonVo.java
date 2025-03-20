package com.nextgen.gameaggregator.vendor.playtech.vo;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonVo implements HttpResponse {

    private String requestId;
    @NotBlank(message = "url can not be blank")
    private ErrorVo data;
    private ErrorVo error;

    public boolean hasError() {
        return error != null;
    }
}