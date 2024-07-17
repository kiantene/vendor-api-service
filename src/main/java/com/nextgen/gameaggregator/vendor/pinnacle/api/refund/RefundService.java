package com.nextgen.gameaggregator.vendor.pinnacle.api.refund;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.exception.*;
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

import java.util.Objects;

@Service
@Slf4j
public class RefundService {
    private final SportWalletService sportWalletService;

    @Autowired
    public RefundService(SportWalletService sportWalletService) {

        this.sportWalletService = sportWalletService;
    }

    public CommonVo refund(WalletRequest walletRequest, Action action, CommonVo commonVo) throws
            InvalidPlayerException, BetNotAllowedException, BetResultIdempotentViolationException,
            TransactionStillProcessingException, BetNotFoundException, InvalidOperatorResponseException, InvalidRequestException {

        ActionsTransactionDto transactionDto = action.getTransaction();
        ActionsWagerInfoDto wagerInfoDto = action.getWagerInfo();
        this.dataMapper(walletRequest, wagerInfoDto, transactionDto);

        walletRequest = sportWalletService.refund(walletRequest);
        commonVo.setBalance(walletRequest.getBalanceAfter());

        return commonVo;
    }

    private void dataMapper(WalletRequest walletRequest, ActionsWagerInfoDto wagerInfoDto, ActionsTransactionDto transactionDto) {
        walletRequest.setVendorBetId(wagerInfoDto.getWagerId().toString());
        walletRequest.setRoundId(Objects.requireNonNullElse(wagerInfoDto.getWagerMasterId(), wagerInfoDto.getWagerId()).toString());
        String transactionDate = transactionDto.getTransactionDate(); // TODO : need do checking if transactionDto is null
        Long timestamp = VendorService.convertDateTimeStringToTimestamp(transactionDate, Formats.DATE_TIME_FORMAT_T_SEPARATOR);
        walletRequest.setTimestamp(timestamp);
    }
}
