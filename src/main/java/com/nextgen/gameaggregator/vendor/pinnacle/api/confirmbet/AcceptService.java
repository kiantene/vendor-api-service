package com.nextgen.gameaggregator.vendor.pinnacle.api.confirmbet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
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

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class AcceptService {
    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;
    @Autowired
    private SportUnsettledBetService sportUnsettledBetService;

    public CommonVo accept(Action action, GameSession gameSession, HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();
        Long transactionId = Optional.ofNullable(action.getTransaction()).map(ActionsTransactionDto::getTransactionId).orElse(null);
        Long wagerId = Optional.ofNullable(action.getWagerInfo()).map(ActionsWagerInfoDto::getWagerId).orElse(null);
        CommonVo commonVo = new CommonVo(action.getId(), transactionId, wagerId);

        try {
            AcceptDto acceptDto = new ModelMapper().map(action.getWagerInfo(), AcceptDto.class);
            // check if vendor return Bet Amount else get Bet Amount from Couchbase Unsettled Bet
            this.updateBetAmount(acceptDto, gameSession);
            // if dto contains "Transaction" , update new bet amount value = (old bet amount - transaction[amount])
            this.updateVendorNewBetAmount(acceptDto, action);
            BetEvent response = sportWalletService.confirmBet(traceId, gameSession, acceptDto, httpRequestLog.getRequestBody(), httpRequestLog);
            commonVo.setBalance(response.getLastBalance());

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
        }

        // for Testing
        if (action.getPlayerInfo().getUserCode().equalsIgnoreCase("PX1420004N")) {
            commonVo.setSetResponseVoErrorCode(Boolean.TRUE);
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
        }
        if (action.getPlayerInfo().getUserCode().equalsIgnoreCase("PX1420004R")) {
            commonVo.setSetResponseVoErrorCode(Boolean.FALSE);
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
        }

        return commonVo;
    }

    private void updateBetAmount(AcceptDto acceptDto, GameSession gameSession) throws BetNotFoundException {
        if (Objects.nonNull(acceptDto.getStake())) {
            acceptDto.setBetAmount(acceptDto.getStake());
        } else {
            SportUnsettledBetCouchbase sportUnsettledBetCouchbase = sportUnsettledBetService.couchbaseGetByExternalTransactionId(gameSession.getVendorPlayerUsername(), acceptDto.getExternalTransactionId());
            acceptDto.setBetAmount(sportUnsettledBetCouchbase.getBetAmount());
        }
    }

    private void updateVendorNewBetAmount(AcceptDto acceptDto, Action action) {
        Optional.ofNullable(action.getTransaction()).ifPresent(data -> {
            if (Objects.nonNull(acceptDto.getBetAmount()) && Objects.nonNull(data.getAmount())) {
                acceptDto.setVendorNewBetAmount(acceptDto.getBetAmount().subtract(data.getAmount()));
            }
        });
    }
}
