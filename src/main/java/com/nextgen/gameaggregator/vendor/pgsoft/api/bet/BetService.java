package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.service.BetHistoryService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Bool;

@Service
public class BetService {
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private BetHistoryService betHistoryService;

    public void process(String traceId, GameSession gameSession, BetData betData, String body)
            throws
            InsufficientBalanceException, DuplicateExternalTransactionIdException,
            InvalidOperatorResponseException, InvalidAgentApiCredentialException {

        walletService.processBet(traceId, gameSession, betData, body);
    }

    public Boolean shouldReprocess(CashTransferInOutDto dto) throws InvalidPlayerException, GameNotSupportedException {
        Boolean shouldReprocess = false;
        try {
            betHistoryService.getBetTransactionByVendorTransactionId(dto.getExternalTransactionId(), 3);
        } catch (BetNotFoundException betNotFoundException) {
            shouldReprocess = true;
        }
        //
        return shouldReprocess;
    }

}
