package com.nextgen.gameaggregator.enums;

public enum GameRoundState {
    PENDING,    // the action is pending for processing
    UNSETTLED,  // the round has not been settled yet
    SETTLED,    // the round has been settled
    REFUNDED,   // the round was refunded to players
    VOID,       // the round was void due to wrong game logic
    COMPLETED   // the action has completed processing
    ;
}
