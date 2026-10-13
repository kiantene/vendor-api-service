package com.nextgen.gameaggregator.core.engine.operator.wallet.debit;

import com.nextgen.gameaggregator.core.engine.wallet.debit.DebitContext;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Translates a {@link DebitContext} + {@link DebitTxn} into the outbound
 * {@link OperatorDebitRequest} the operator expects.
 */
@Component
@RequiredArgsConstructor
public class OperatorDebitRequestMapper {

    public OperatorDebitRequest toOperatorRequest(DebitContext context, DebitTxn txn) {
        OperatorDebitRequest dto = new OperatorDebitRequest();

        dto.setTraceId(context.getTraceId());
        dto.setTransactionId(txn.getGaTxnId());
        dto.setUsername(context.getAgentPlayerUsername());
        dto.setCurrency(context.getCurrencyCode());
        dto.setToken(context.getToken());
        dto.setGameCode(context.getGameCode());
        dto.setRoundId(context.getVendorRoundId());
        dto.setTimestamp(context.getVendorTimestamp());

        TxnAmount txnAmount = TxnAmount.of(context.getVendorAmount(), context.getFromVendorRate());
        dto.setAmount(txnAmount.amount());

        return dto;
    }
}
