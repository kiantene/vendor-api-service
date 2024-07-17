package com.nextgen.gameaggregator.vendor.pinnacle.api.unsettle;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Formats;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.Action;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class UnsettleService {
    private final SportWalletService sportWalletService;

    @Autowired
    public UnsettleService(SportWalletService sportWalletService) {

        this.sportWalletService = sportWalletService;
    }

    public CommonVo unsettle(WalletRequest walletRequest, Action action, CommonVo commonVo) throws
            BetResultIdempotentViolationException, TransactionStillProcessingException, BetNotFoundException,
            InvalidOperatorResponseException, InvalidRequestException, BetNotAllowedException,
            InvalidPlayerException {

        ActionsWagerInfoDto wagerInfoDto = action.getWagerInfo();
        this.dataMapper(walletRequest, wagerInfoDto);

        walletRequest = sportWalletService.unsettle(walletRequest);
        commonVo.setBalance(walletRequest.getBalanceAfter());

        return commonVo;
    }

    private void dataMapper(WalletRequest walletRequest, ActionsWagerInfoDto wagerInfoDto) {
        walletRequest.setVendorBetId(wagerInfoDto.getWagerId().toString());
        walletRequest.setRoundId(Objects.requireNonNullElse(wagerInfoDto.getWagerMasterId(), wagerInfoDto.getWagerId()).toString());
        String dateTimeString = wagerInfoDto.getTransactionDate();
        walletRequest.setTimestamp(VendorService.convertDateTimeStringToTimestamp(dateTimeString, Formats.DATE_TIME_FORMAT_T_SEPARATOR));
    }
}
