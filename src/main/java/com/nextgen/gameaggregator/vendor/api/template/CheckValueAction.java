package com.nextgen.gameaggregator.vendor.api.template;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.vendor.grpc.v1.subcriber.OperatorWalletBalanceGrpc;
import com.nextgen.sas.core.web.action.Action;
import com.nextgen.sas.core.web.action.WebActionRequest;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = Constant.SAMPLE_CHECK_VALUE_ACTION)
public class CheckValueAction extends Action implements WebActionRequest {

    @Autowired
    private OperatorWalletBalanceGrpc operatorWalletBalanceGrpc;

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> action(@RequestBody ObjectNode json, HttpServletRequest request) {


        Map<String, Object> bodyValues = new HashMap<String, Object>();
        bodyValues.put("WEB_ACTION", com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.WEB_ACTION);
        bodyValues.put("ACTION_AUTHENTICATE", com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.ACTION_AUTHENTICATE);
        bodyValues.put("ACTION_BALANCE", com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.ACTION_BALANCE);
        bodyValues.put("ACTION_BET", com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.ACTION_BET);
        bodyValues.put("ACTION_BONUS_WIN", com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.ACTION_BONUS_WIN);
        bodyValues.put("ACTION_PROMO_WIN", com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.ACTION_PROMO_WIN);

        return ResponseEntity.ok(bodyValues);
    }

    @Override
    public String verify(WebRequestWrapper request) {
        return null;
    }
}
