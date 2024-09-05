package com.nextgen.gameaggregator.vendor.cg.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceVo implements HttpResponse {
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
    public String returnTime; //"2019-09-19T11:23:39.157+08:00"



    @Override
    public boolean hasError() {
        return false;
    }
}
