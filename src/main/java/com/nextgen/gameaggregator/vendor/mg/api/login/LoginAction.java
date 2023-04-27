package com.nextgen.gameaggregator.vendor.mg.api.login;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nextgen.gameaggregator.vendor.mg.constant.Endpoints;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class LoginAction {
    @PostMapping(path = Endpoints.LOGIN)
    public ResponseEntity<LoginVo> login() {
        String currency = "USD";
        BigDecimal balance = new BigDecimal("1000");
        String extOperatorToken = "ABCDEFG123456";
        
        LoginVo loginVo = new LoginVo();
        loginVo.setCurrency(currency);
        loginVo.setBalance(balance);
        loginVo.setExtOperatorToken(extOperatorToken);
        
        return new ResponseEntity<>(loginVo, HttpStatus.OK);
    }
}
