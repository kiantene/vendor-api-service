package com.nextgen.gameaggregator.vendor.marblex.api.controller;

import com.nextgen.gameaggregator.vendor.marblex.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.marblex.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.marblex.api.cancel.CancelService;
import com.nextgen.gameaggregator.vendor.marblex.api.resettle.ResettleService;
import com.nextgen.gameaggregator.vendor.marblex.api.result.ResultService;
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
    public final ResultService resultService;
    public final ResettleService resettleService;
    public final CancelService cancelService;

    @Autowired
    public ActionController(BalanceService balanceService, BetService betService, ResultService resultService, ResettleService resettleService, CancelService cancelService) {
        this.balanceService = balanceService;
        this.betService = betService;
        this.resultService = resultService;
        this.resettleService = resettleService;
        this.cancelService = cancelService;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public CommonVo balance(HttpServletRequest request) {
        return this.balanceService.getBalance(request);
    }

    @PostMapping(path = EndPoints.BET)
    public CommonVo bet(HttpServletRequest request) {
        return this.betService.placeBet(request);
    }

    @PostMapping(path = EndPoints.RESULT)
    public CommonVo result(HttpServletRequest request) {
        return this.resultService.settleBet(request);
    }

    @PostMapping(path = EndPoints.CANCEL)
    public CommonVo cancel(HttpServletRequest request) {
        return this.cancelService.cancelBet(request);
    }

    @PostMapping(path = EndPoints.RESETTLE)
    public CommonVo resettle(HttpServletRequest request) {
        return this.resettleService.resettle(request);
    }

}
