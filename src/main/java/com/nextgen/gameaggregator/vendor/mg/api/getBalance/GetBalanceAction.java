package com.nextgen.gameaggregator.vendor.mg.api.getBalance;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nextgen.gameaggregator.vendor.mg.constant.Endpoints;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class GetBalanceAction {
    @PostMapping(path = Endpoints.GET_BALANCE)
    public ResponseEntity<GetBalanceVo> getBalance() {
        String currency = "USD";
        BigDecimal balance = new BigDecimal("1000");
        
        GetBalanceVo getBalanceVo = new GetBalanceVo();
        getBalanceVo.setCurrency(currency);
        getBalanceVo.setBalance(balance);
        
        return new ResponseEntity<>(getBalanceVo, HttpStatus.OK);
    }
}
