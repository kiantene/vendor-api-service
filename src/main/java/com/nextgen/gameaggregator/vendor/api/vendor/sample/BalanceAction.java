package com.nextgen.gameaggregator.vendor.api.vendor.sample;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.grpc.v1.operator.walletbalance.WalletBalanceVo;
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
import java.util.UUID;

@RestController
@RequestMapping(path = Constant.SAMPLE_WALLET_BALANCE_ACTION)
public class BalanceAction extends Action implements WebActionRequest {

    @Autowired
    private OperatorWalletBalanceGrpc operatorWalletBalanceGrpc;

    @PostMapping("")
    public ResponseEntity< Map<String, Object> > action(@RequestBody ObjectNode json , HttpServletRequest request) {

        WalletBalanceVo serviceVo = this.operatorWalletBalanceGrpc.walletBalance(
                json.get("agentId").asLong(),
                json.get("agentPlayerId").asLong(),
                json.get("vendorId").asLong(),
                json.get("currency").asText(),
                UUID.randomUUID().toString(),
                json.get("agentCredentialId").asLong()
        );
        System.out.println(serviceVo.getBalance());
        System.out.println(serviceVo.getStatus());
        System.out.println(serviceVo.getOperatorErrorMessage());
        System.out.println(serviceVo.getOperatorErrorCode());

        Map<String, Object> bodyValues = new HashMap<String, Object>();
        bodyValues.put("status", serviceVo.getStatus());
        bodyValues.put("balance", serviceVo.getBalance());
        bodyValues.put("operatorErrorCode", serviceVo.getOperatorErrorCode());
        bodyValues.put("operatorErrorMessage", serviceVo.getOperatorErrorMessage());

        return ResponseEntity.ok(bodyValues);
    }
    @Override
    public String verify(WebRequestWrapper request) {
        return null;
    }
}
