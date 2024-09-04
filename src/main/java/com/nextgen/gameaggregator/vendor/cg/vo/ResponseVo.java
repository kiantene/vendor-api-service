package com.nextgen.gameaggregator.vendor.cg.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseVo implements HttpResponse {

    @NotBlank
    public String channelId; //"52772"
    @NotBlank
    public String accountId; //"abctesting123"
    @NotBlank
    public BigDecimal balance; //1000.00
    @NotBlank
    public String currency; //"USD"
    @NotBlank
    public Integer errorCode; //0,
    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{2}:\\d{2}$")
    public String returnTime;

    @Override
    public boolean hasError() {
        return false;
    }
}
