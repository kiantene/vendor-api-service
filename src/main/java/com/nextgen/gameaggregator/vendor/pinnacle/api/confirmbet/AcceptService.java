package com.nextgen.gameaggregator.vendor.pinnacle.api.confirmbet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.service.SportUnsettledBetService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.Action;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsTransactionDto;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class AcceptService {
    private final SportWalletService sportWalletService;
    private final GameSessionService gameSessionService;
    private final WalletRequestService walletRequestService;
    private final SportUnsettledBetService sportUnsettledBetService;

    @Autowired
    public AcceptService(SportWalletService sportWalletService,
                         GameSessionService gameSessionService,
                         WalletRequestService walletRequestService,
                         SportUnsettledBetService sportUnsettledBetService) {

        this.sportWalletService = sportWalletService;
        this.gameSessionService = gameSessionService;
        this.walletRequestService = walletRequestService;
        this.sportUnsettledBetService = sportUnsettledBetService;
    }

    private static String getPinnacleGameCode(ActionsWagerInfoDto wagerInfoDto) {
        return Optional.ofNullable(wagerInfoDto.getSportId())
                .map(String::valueOf)
                .orElseGet(() ->
                        Optional.ofNullable(wagerInfoDto.getLegs())
                                .filter(legs -> !legs.isEmpty())
                                .map(legs -> String.valueOf(legs.get(0).getSportId()))
                                .orElse(null)
                );
    }


    public CommonVo accept(WalletRequest walletRequest, Action action) throws
            BetResultIdempotentViolationException, TransactionStillProcessingException,
            InvalidOperatorResponseException, BetNotFoundException, BetNotAllowedException, InvalidRequestException, InvalidPlayerException {

        ActionsWagerInfoDto wagerInfoDto = action.getWagerInfo();
        ActionsTransactionDto transactionDto = action.getTransaction();
        Long wagerId = wagerInfoDto.getWagerId();
        Long transactionId = Optional.ofNullable(transactionDto).map(ActionsTransactionDto::getTransactionId).orElse(null);
        String externalTransactionId = action.getId().toString();
        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();

        CommonVo commonVo = new CommonVo(action.getId(), transactionId, wagerId);
        walletRequestService.updateByVendorUsername(walletRequest, vendorPlayerUsername);

        this.dataMapper(walletRequest, externalTransactionId, wagerInfoDto, transactionDto);

        walletRequest = sportWalletService.confirmBet(walletRequest);
        commonVo.setBalance(walletRequest.getBalanceAfter());

        return commonVo;
    }

    private void dataMapper(WalletRequest walletRequest, String externalTransactionId, ActionsWagerInfoDto wagerInfoDto, ActionsTransactionDto transactionDto) throws BetNotFoundException, InvalidRequestException {
        walletRequest.setExternalTransactionId(externalTransactionId);
        walletRequest.setVendorBetId(wagerInfoDto.getWagerId().toString());
        walletRequest.setNewVendorBetId(wagerInfoDto.getWagerId().toString());
        walletRequest.setRoundId(wagerInfoDto.getWagerId().toString());
        walletRequest.setVendorGameCode(getPinnacleGameCode(wagerInfoDto));

        if (isMultipleBet(wagerInfoDto)) {
            walletRequest.setVendorBetId(wagerInfoDto.getWagerMasterId().toString() + "_" + wagerInfoDto.getWagerNum());
            walletRequest.setRoundId(wagerInfoDto.getWagerMasterId().toString());
            walletRequest.setNewVendorBetId(wagerInfoDto.getWagerId().toString());
        }

        this.updateBetAmount(walletRequest);

        // if dto contains "Transaction" , update new bet amount value = (old bet amount - transaction[amount])
        this.updateNewBetAmount(walletRequest, transactionDto, wagerInfoDto);
    }

    private void updateBetAmount(WalletRequest walletRequest) throws BetNotFoundException {
        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        String vendorBetId = walletRequest.getVendorBetId();
        SportUnsettledBet sportUnsettledBetCouchbase = sportUnsettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);
        walletRequest.setBetAmount(sportUnsettledBetCouchbase.getBetAmount());
    }

    private boolean isMultipleBet(ActionsWagerInfoDto wagerInfoDto) {
        boolean isMultipleBet = false;
        if (Objects.nonNull(wagerInfoDto.getWagerMasterId())) {
            isMultipleBet = !wagerInfoDto.getWagerId().toString().equalsIgnoreCase(wagerInfoDto.getWagerMasterId().toString());
        }
        return isMultipleBet;
    }

    private void updateNewBetAmount(WalletRequest walletRequest, ActionsTransactionDto transactionDto, ActionsWagerInfoDto wagerInfoDto) throws InvalidRequestException {

        BigDecimal placeBetBetAmount = walletRequest.getBetAmount();
        BigDecimal acceptBetBetAmount = wagerInfoDto.getStake();

        // If the accepted bet amount is different from the placed bet amount, update the new bet amount
        if (Objects.nonNull(acceptBetBetAmount) && acceptBetBetAmount.compareTo(placeBetBetAmount) != 0) {
            walletRequest.setNewBetAmount(acceptBetBetAmount);
        }

        // If transactionDto is null or transactionAmount is null, do nothing
        if (transactionDto == null || transactionDto.getAmount() == null) {
            return;
        }
        BigDecimal transactionAmount = transactionDto.getAmount();

        // If transaction amount is negative, throw an exception
        if (transactionAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("Transaction amount cannot be negative");
        }
        // Update new bet amount by subtracting the transaction amount from the placed bet amount
        walletRequest.setNewBetAmount(placeBetBetAmount.subtract(transactionAmount));
    }

}
