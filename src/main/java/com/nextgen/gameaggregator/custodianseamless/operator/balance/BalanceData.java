package com.nextgen.gameaggregator.custodianseamless.operator.balance;

import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class BalanceData {

    private String username;
    private String currencyCode;
    private BigDecimal amount;
    private Long timestamp;

    public BalanceData( AgentPlayer agentPlayer, Currency currency){
        this.username = agentPlayer.getUsername();
        this.currencyCode = currency.getCode();
    }
}
