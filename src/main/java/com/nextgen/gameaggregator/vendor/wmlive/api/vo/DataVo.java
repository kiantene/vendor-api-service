package com.nextgen.gameaggregator.vendor.wmlive.api.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.wmlive.api.betandsettle.PointInOutDto;
import com.nextgen.gameaggregator.vendor.wmlive.api.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.wmlive.api.rollback.TimeoutBetReturnDto;
import lombok.Data;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.nextgen.gameaggregator.vendor.wmlive.constant.Formats.DATE_TIME_FORMAT;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataVo {
    private String user;
    private String money;
    private String responseDate;
    private String dealid;
    private String cash;

    public DataVo(PointInOutDto dto, BigDecimal balance) {
        this.user = dto.getUser();
        this.money = dto.getMoney().setScale(2, RoundingMode.DOWN).toString();
        this.responseDate = new LocalDateTime().toString(DATE_TIME_FORMAT);
        this.dealid = dto.getDealid();
        this.cash = balance.setScale(2, RoundingMode.DOWN).toString();
    }

    public DataVo(TimeoutBetReturnDto dto, BigDecimal balance) {
        this.user = dto.getUser();
        this.money = dto.getMoney().setScale(2, RoundingMode.DOWN).toString();
        this.responseDate = new LocalDateTime().toString(DATE_TIME_FORMAT);
        this.dealid = dto.getDealid();
        this.cash = balance.setScale(2, RoundingMode.DOWN).toString();
    }

    public DataVo(CommonDto dto, BigDecimal balance) {
        this.user = dto.getUser();
        this.responseDate = new LocalDateTime().toString(DATE_TIME_FORMAT);
        this.money = balance.setScale(2, RoundingMode.DOWN).toString();
    }


}
