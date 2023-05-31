package com.nextgen.gameaggregator.vendor.queenmaker.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsersVo extends ResponseVo {
    private String userid;
    private Integer err;
    private String errdesc;
    private List<WalletsVo> wallets;
}
