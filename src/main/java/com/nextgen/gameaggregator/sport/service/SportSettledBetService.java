package com.nextgen.gameaggregator.sport.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import com.nextgen.gameaggregator.sport.repository.SportSettledBetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class SportSettledBetService {

    private final SportSettledBetRepository sportSettledBetRepository;

    public SportSettledBetService(SportSettledBetRepository sportSettledBetRepository) {

        this.sportSettledBetRepository = sportSettledBetRepository;
    }

    public SportSettledBet save(SportSettledBet sportSettledBet) {
        sportSettledBetRepository.save(sportSettledBet);
        return sportSettledBet;
    }

    public void delete(SportSettledBet sportSettledBet) {
        sportSettledBetRepository.delete(sportSettledBet);
    }

    public SportSettledBet getByVendorPlayerUsernameAndVendorBetId(String vendorPlayerUsername, String vendorBetId) throws BetNotFoundException {
        SportSettledBet sportSettledBet = null;

        sportSettledBet = sportSettledBetRepository.findByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);

        if (Objects.isNull(sportSettledBet)) {
            sportSettledBet = sportSettledBetRepository.findById(vendorPlayerUsername + '_' + vendorBetId).orElse(null);
        }
        
        if (sportSettledBet == null) { // No matching bet record
            throw new BetNotFoundException("Cannot find sportSettledBet getByVendorPlayerUsernameAndVendorBetId: " + vendorPlayerUsername + ", " + vendorBetId);
        }

        return sportSettledBet;
    }

    public SportSettledBet idempotentCheck(WalletRequest walletRequest) throws BetResultIdempotentViolationException, TransactionStillProcessingException {

        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        String vendorBetId = walletRequest.getVendorBetId();
        String externalTransactionId = walletRequest.getExternalTransactionId();
        SportSettledBet sportSettledBet = null;

        try {
            sportSettledBet = this.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);

            if (!sportSettledBet.getExternalTransactionId().equals(externalTransactionId)) {
                // if settledBet is found but externalTransactionId is not matched, then is new status changed of this bet
                return sportSettledBet;
            }

            if (sportSettledBet.getStatus().equals(ResponseCodes.Status.SC_OK.code)) {
                throw new BetResultIdempotentViolationException("Process settle idempotent: " + vendorPlayerUsername + '_' + externalTransactionId);
            } else if (sportSettledBet.getStatus().equals(ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code)) {
                throw new TransactionStillProcessingException();
            }

        } catch (BetNotFoundException betNotFoundException) {
            // If the bet is not found in sportSettledBet, means this is not a duplicate bet and need to process as per normal
        }

        return sportSettledBet;
    }
}
