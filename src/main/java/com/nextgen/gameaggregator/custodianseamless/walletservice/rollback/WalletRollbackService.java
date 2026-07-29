package com.nextgen.gameaggregator.custodianseamless.walletservice.rollback;

public interface WalletRollbackService {
    void process(WalletRollbackContext context);

    WalletRollbackService initialise(WalletRollbackContext context);

}
