package com.nextgen.gameaggregator.vendor.queenmaker.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.ResponseVo;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceVo extends ResponseVo {
    private List<UsersVo> users;

    @Override
    public boolean hasError() {
        return false;
    }
}
