package com.nextgen.gameaggregator.vendor.api.template;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.grpc.v1.operator.betrefund.BetRefundGrpcVo;
import com.nextgen.gameaggregator.vendor.grpc.v1.subcriber.OperatorBetRefundGrpc;
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
@RequestMapping(path = Constant.SAMPLE_BET_REFUND_ACTION)
public class RefundAction  extends Action implements WebActionRequest {

    @Autowired
    private OperatorBetRefundGrpc operatorBetRefundGrpc;

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> action(@RequestBody ObjectNode json , HttpServletRequest request) {
        BetRefundGrpcVo serviceVo = this.operatorBetRefundGrpc.betRefund(
                json.get("agentId").asLong(),
                json.get("agentPlayerId").asLong(),
                json.get("gameId").asLong(),
                json.get("currency").asText(),
                UUID.randomUUID().toString(),
                json.get("agentCredentialId").asLong(),
                UUID.randomUUID().toString(),
                json.get("externalBetId").asText(),
                json.get("externalRoundId").asText(),
                json.get("betAmount").asDouble(),
                json.get("refundAmount").asDouble(),
                json.get("resultType").asInt(),
                json.get("betTime").asLong(),
                json.get("settledTime").asLong()
        );

        Map<String, Object> bodyValues = new HashMap<String, Object>();
        bodyValues.put("status", serviceVo.getStatus());
        bodyValues.put("balance", serviceVo.getBalance());
        bodyValues.put("beforeBalance", serviceVo.getBeforeBalance());
        bodyValues.put("operatorErrorCode", serviceVo.getOperatorErrorCode());
        bodyValues.put("operatorErrorMessage", serviceVo.getOperatorErrorMessage());

        return ResponseEntity.ok(bodyValues);
    }
    @Override
    public String verify(WebRequestWrapper request) {
        return null;
    }
}
