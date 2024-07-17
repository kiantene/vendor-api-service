package com.nextgen.gameaggregator.vendor.pinnacle.api.settled;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBet;
import com.nextgen.gameaggregator.sport.service.SportUnsettledBetService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Formats;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.Action;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsTransactionDto;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class SettledService {
    private final SportWalletService sportWalletService;
    private final SportUnsettledBetService sportUnsettledBetService;

    @Autowired
    public SettledService(SportWalletService sportWalletService,
                          SportUnsettledBetService sportUnsettledBetService) {

        this.sportWalletService = sportWalletService;
        this.sportUnsettledBetService = sportUnsettledBetService;
    }

    public CommonVo settled(WalletRequest walletRequest, Action action, CommonVo commonVo) throws
            InvalidPlayerException, BetNotAllowedException, BetResultIdempotentViolationException,
            TransactionStillProcessingException, BetNotFoundException, InvalidOperatorResponseException,
            InvalidRequestException, BetFailedException {

        ActionsWagerInfoDto wagerInfoDto = action.getWagerInfo();
        BigDecimal winAmount = Optional.ofNullable(action.getTransaction()).map(ActionsTransactionDto::getAmount).orElse(BigDecimal.ZERO);
        this.dataMapper(walletRequest, wagerInfoDto, winAmount);
        this.checkIsConfirmBetOrIsUnsettledBet(walletRequest);

        walletRequest = sportWalletService.settle(walletRequest);
        commonVo.setBalance(walletRequest.getBalanceAfter());

        return commonVo;
    }

    private void checkIsConfirmBetOrIsUnsettledBet(WalletRequest walletRequest) throws BetFailedException, BetNotFoundException {
        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();
        String vendorBetId = walletRequest.getVendorBetId();

        SportUnsettledBet sportUnsettledBet = sportUnsettledBetService.getByVendorPlayerUsernameAndVendorBetId(vendorPlayerUsername, vendorBetId);
        Integer isConfirmBet = Objects.requireNonNullElse(sportUnsettledBet.getIsConfirmBet(), 0);
        Integer isUnsettledBet = Objects.requireNonNullElse(sportUnsettledBet.getIsUnsettledBet(), 0);
        if (!isConfirmBet.equals(1) && !isUnsettledBet.equals(1))
            throw new BetFailedException("Bet External Transaction Id : " + walletRequest.getExternalTransactionId() + " not confirmed bet.");
    }

    private void dataMapper(WalletRequest walletRequest, ActionsWagerInfoDto wagerInfoDto, BigDecimal winAmount) {
        walletRequest.setVendorBetId(wagerInfoDto.getWagerId().toString());
        walletRequest.setRoundId(Objects.requireNonNullElse(wagerInfoDto.getWagerMasterId(), wagerInfoDto.getWagerId()).toString());
        walletRequest.setWinAmount(winAmount);
        walletRequest.setWinLoss(wagerInfoDto.getProfitAndLoss()); // if not set will calculate in SportSettleBetProcessor
        walletRequest.setVendorBetTime(System.currentTimeMillis());
        String dateTimeString = Objects.requireNonNullElse(wagerInfoDto.getResettlementTime(), wagerInfoDto.getSettlementTime());
        walletRequest.setVendorSettleTime(VendorService.convertDateTimeStringToTimestamp(dateTimeString, Formats.DATE_TIME_FORMAT));
        walletRequest.setBetStatus(BetStatus.SETTLED);
        walletRequest.setBetType(wagerInfoDto.getType().equalsIgnoreCase("PARLAY") ? BetType.PARLAY_BET.code : BetType.NORMAL_BET.code);
    }
}
