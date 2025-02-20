package com.nextgen.gameaggregator.vendor.marblex.api.controller;

import com.nextgen.gameaggregator.vendor.marblex.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.marblex.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ActionController {
    public final BalanceService balanceService;
    @Autowired
    public ActionController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public CommonVo balance(HttpServletRequest request) {
        return this.balanceService.getBalance(request);
    }

}
