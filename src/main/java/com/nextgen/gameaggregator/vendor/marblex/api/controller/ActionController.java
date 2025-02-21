package com.nextgen.gameaggregator.vendor.marblex.api.controller;

import com.nextgen.gameaggregator.vendor.marblex.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.marblex.api.bet.BetService;
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
    public final BetService betService;
    @Autowired
    public ActionController(BalanceService balanceService, BetService betService) {
        this.balanceService = balanceService;
        this.betService = betService;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public CommonVo balance(HttpServletRequest request) {
        return this.balanceService.getBalance(request);
    }

    @PostMapping(path = EndPoints.BET)
    public CommonVo bet(HttpServletRequest request) {
        return this.betService.placeBet(request);
    }


}
