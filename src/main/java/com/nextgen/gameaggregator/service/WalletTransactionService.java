package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.exception.InternalServerException;

public interface WalletTransactionService {

    WalletTransaction prepareEntity(WalletRequest walletRequest, String action);

    WalletTransaction save(WalletTransaction walletTransaction) throws InternalServerException;

    WalletTransaction updateOperatorStatus(String id, WalletRequest walletRequest);

    WalletTransaction getByRoundIdAndVendorPlayerUsername(String roundId, String vendorPlayerUsername);

    WalletTransaction getByVendorIdAndExternalTransactionId(Integer vendorId, String externalTransactionId);
}
