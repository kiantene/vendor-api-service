package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.enums.BetTransactionType;

import java.math.BigDecimal;

public record TransactionIntent(
        BetTransactionType type,
        BigDecimal amount,
        String gaBetId
) {}