package com.nextgen.gameaggregator.core.webclient;

import com.nextgen.core.api.ApiRequest;
import com.nextgen.gameaggregator.core.retry.Partitionable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OperatorApiRequest extends ApiRequest implements Partitionable {

    private final Integer agentId;
    private final String agentPlayerUsername;
    private final Long transactionTime;

    @Override
    public String getPartitionKey() {
        return this.agentPlayerUsername;
    }

    @Override
    public Long getTransactionTime() {
        return this.transactionTime;
    }
}
