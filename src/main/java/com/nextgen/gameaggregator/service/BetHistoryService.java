package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.DuplicateExternalTransactionIdException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceAction;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetAction;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetDto;
import com.nextgen.gameaggregator.operator.wallet.bet.WalletBetVo;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinAction;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinDto;
import com.nextgen.gameaggregator.operator.wallet.win.WalletWinVo;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BetHistoryService {
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private BetHistoryRepository betHistoryRepository;
    @Autowired
    private BetResultLogRepository betResultLogRepository;


    /**
     * Check for a duplicate vendor transaction Id
     *
     * @param txnId Vendor's unique Id for each transaction
     * @param gameId Game Id within Game Aggregator System
     * @param vendorPlayerId Id of the player in VendorPlayer
     * @throws DuplicateExternalTransactionIdException If a matching external_transaction_id is found.
     */
    // TODO: performance tuning, read from cache
    public void checkDuplicateExternalTransaction(String txnId, Integer gameId, Long vendorPlayerId) throws DuplicateExternalTransactionIdException {
        BetResultLog resultLog = betResultLogRepository.findByExternalTransactionIdAndVendorGameIdAndVendorPlayerId(txnId, gameId, vendorPlayerId);
        if (resultLog != null) { // Found a matching external transaction Id
            throw new DuplicateExternalTransactionIdException("Duplicate external transaction Id: " + txnId);
        }
    }

    /**
     * Retrieve a bet transaction record based on vendor's round Id
     *
     * @param roundId Vendor's round Id
     * @param gameId Game Id within Game Aggregator System
     * @param vendorPlayerId Id of the player in VendorPlayer
     * @throws BetNotFoundException If no bet record is found
     * @return BetHistory containing all information of a single Bet
     */
    // TODO: performance tuning, read from cache
    public BetHistory getBetTransaction(String roundId, Integer gameId, Long vendorPlayerId) throws BetNotFoundException {
        BetHistory betHistory = betHistoryRepository.findByRoundIdAndVendorGameIdAndVendorPlayerId(roundId, gameId, vendorPlayerId);
        if (betHistory == null) { // No matching bet record for the given round Id
            throw new BetNotFoundException("Cannot find round Id: " + roundId);
        }
        return betHistory;
    }
}
