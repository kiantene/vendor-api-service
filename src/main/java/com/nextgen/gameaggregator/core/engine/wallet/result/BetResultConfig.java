package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.Data;

@Data
public class BetResultConfig {
    // controls data pipeline, how do send bet transactions to kafka topic
    public enum ProcessingMode {
        SINGLE,           // default, sends main transaction to topic after successful calls to operator
        BATCH,            // sends multiple child transactions to topic (this will exclude main transaction)
        SINGLE_AND_BATCH, // sends both main and child transactions to topic
        DISABLED;         // disable auto sending, can be handled manually on a separate handler if required

        public boolean isSingleMode() {
            return this == SINGLE || this == SINGLE_AND_BATCH;
        }
        public boolean isBatchMode() {
            return this == BATCH || this == SINGLE_AND_BATCH;
        }
        public boolean isDisabled() {
            return this == DISABLED;
        }
    }

    /**
     * betAndResult = true means this api request contains bet amount and require processing
     * of bet and result together (resultType=BET_WIN | BET_LOSE)
     */
    private boolean betAndResult = false;
    private ResultType resultType;
    private SettleType settleType = SettleType.BET; // Default is settled by bet
    private ProcessingMode processingMode = ProcessingMode.SINGLE;
    private boolean allowResultBeforeBet = false;
    private boolean allowResultWhenRoundHasEnded = true;
    private boolean returnSuccessOnDuplicate = false;
    private boolean rejectResultIfRefunded = false;

    // Chaining methods
    public BetResultConfig betAndResult(boolean flag) {
        this.betAndResult = flag;
        return this;
    }

    public BetResultConfig resultType(ResultType resultType) {
        this.resultType = resultType;
        return this;
    }

    public BetResultConfig settleType(SettleType settleType) {
        this.settleType = settleType;
        return this;
    }

    public BetResultConfig processingMode(ProcessingMode processingMode) {
        this.processingMode = processingMode;
        return this;
    }

    public BetResultConfig allowResultBeforeBet(boolean flag) {
        this.allowResultBeforeBet = flag;
        return this;
    }

    public BetResultConfig allowResultWhenRoundHasEnded(boolean flag) {
        this.allowResultWhenRoundHasEnded = flag;
        return this;
    }

    public BetResultConfig returnSuccessOnDuplicate(boolean flag) {
        this.returnSuccessOnDuplicate = flag;
        return this;
    }

    public BetResultConfig rejectResultIfRefunded(boolean flag) {
        this.rejectResultIfRefunded = flag;
        return this;
    }

    public boolean isSettledByBet() {
        return settleType == SettleType.BET;
    }

    public boolean isSettledByRound() {
        return settleType == SettleType.ROUND;
    }
}
