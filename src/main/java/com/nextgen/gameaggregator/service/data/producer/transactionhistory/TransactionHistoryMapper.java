package com.nextgen.gameaggregator.service.data.producer.transactionhistory;

import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.GameCategory;
import com.nextgen.gameaggregator.core.entity.Vendor;
import com.nextgen.gameaggregator.entity.ga.BetTransactionHistory;
import com.nextgen.gameaggregator.enums.BetTransactionType;
import org.springframework.stereotype.Component;

@Component
public class TransactionHistoryMapper {

    public BetTransactionHistory from(TransactionHistoryContext context, Agent agent, Vendor vendor, GameCategory gameCategory, BetTransactionType type) {
        BetTransactionHistory transactionHistory = new BetTransactionHistory();

        transactionHistory.setTransactionType(type.name());

        // Vendor
        transactionHistory.setExternalTransactionId(context.externalTransactionId());
        transactionHistory.setVendorBetId(context.vendorBetId());
        transactionHistory.setRoundId(context.roundId());
        transactionHistory.setVendorGameId(context.vendorGameId());
        transactionHistory.setVendorPlayerId(context.vendorPlayerId());
        transactionHistory.setVendorPlayerUsername(context.vendorPlayerUsername());
        transactionHistory.setVendorId(context.vendorId());
        transactionHistory.setVendorLineId(context.vendorLineId());
        transactionHistory.setProductId(vendor.getProductId());
        transactionHistory.setVendorCode(vendor.getCode());

        // Agent
        transactionHistory.setAgentId(context.agentId());
        transactionHistory.setAgentPlayerId(context.agentPlayerId());
        transactionHistory.setAgentPlayerUsername(context.agentPlayerUsername());
        transactionHistory.setHouseId(agent.getHouseId());
        transactionHistory.setMasterAgentId(agent.getMasterAgentId());

        // GA
        transactionHistory.setGameCategoryId(context.gameCategoryId());
        transactionHistory.setGameCode(context.gameCode());
        transactionHistory.setCurrencyId(context.currencyId());
        transactionHistory.setTimestamp(context.timestamp());
        transactionHistory.setGameCategoryCode(gameCategory.getCode());

        // Operator Response
        transactionHistory.setOperatorResponseStatus(context.status());
        transactionHistory.setOperatorTransactionId(context.walletTxnId());

        return transactionHistory;
    }
}
