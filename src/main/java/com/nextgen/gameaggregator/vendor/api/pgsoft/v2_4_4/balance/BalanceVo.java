package com.nextgen.gameaggregator.vendor.api.pgsoft.v2_4_4.balance;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.vo.AbstractActionVo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Data
public class BalanceVo extends AbstractActionVo {

    private ResponseData data;

    public BalanceVo () {
        this.data = new ResponseData();
    }

    @Data
    private class ResponseData {
        private String currencyCode;
        private BigDecimal balanceAmount;
        private Long updatedTime;
    }

    public void setCurrencyCode(String currencyCode) {
        this.data.setCurrencyCode(currencyCode);
    }

    public void setBalanceAmount(BigDecimal balanceAmount) {
        this.data.setBalanceAmount(balanceAmount);
    }

    public void setUpdatedTime(Long updatedTime) {
        this.data.setUpdatedTime(updatedTime);
    }

}