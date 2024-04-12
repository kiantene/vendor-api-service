package com.nextgen.gameaggregator.vendor.pinnacle.api.settled;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.BetFailedException;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import com.nextgen.gameaggregator.sport.service.SportUnsettledBetService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.Action;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsTransactionDto;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class SettledService {
    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;
    @Autowired
    private SportUnsettledBetService sportUnsettledBetService;

    public CommonVo settled(Action action, HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();
        Long transactionId = Optional.ofNullable(action.getTransaction()).map(ActionsTransactionDto::getTransactionId).orElse(null);
        Long wagerId = Optional.ofNullable(action.getWagerInfo()).map(ActionsWagerInfoDto::getWagerId).orElse(null);
        CommonVo commonVo = new CommonVo(action.getId(), transactionId, wagerId);

        try {
            SettledDto settledDto = new ModelMapper().map(action.getWagerInfo(), SettledDto.class);
            settledDto.setVendorPlayerUsername(action.getPlayerInfo().getUserCode());
            settledDto.setTransactionAmount(Optional.ofNullable(action.getTransaction()).map(ActionsTransactionDto::getAmount).orElse(BigDecimal.ZERO));
            // check is confirmed bet or (settled bet -> unsettled bet)
            this.checkIsConfirmBetOrIsUnsettledBet(settledDto);
            BetEvent response = sportWalletService.settle(traceId, settledDto, httpRequestLog);
            commonVo.setBalance(response.getLastBalance());

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
        }

        return commonVo;
    }

    private void checkIsConfirmBetOrIsUnsettledBet(SettledDto settledDto) throws BetFailedException, BetNotFoundException {
        SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.couchbaseGetByExternalTransactionId(settledDto.getVendorPlayerUsername(), settledDto.getExternalTransactionId());
        Integer isConfirmBet = Objects.requireNonNullElse(sportUnsettledBetCouchbase.getIsConfirmBet(), 0);
        Integer isUnsettledBet = Objects.requireNonNullElse(sportUnsettledBetCouchbase.getIsUnsettledBet(), 0);
        if (!isConfirmBet.equals(1) && !isUnsettledBet.equals(1))
            throw new BetFailedException("Bet External Transaction Id : " + settledDto.getExternalTransactionId() + " not confirmed bet.");
    }
}
