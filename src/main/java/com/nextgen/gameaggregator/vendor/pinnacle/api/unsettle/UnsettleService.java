package com.nextgen.gameaggregator.vendor.pinnacle.api.unsettle;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.service.HttpService;
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

import java.util.Optional;

@Service
@Slf4j
public class UnsettleService {
    private final HttpService httpService;
    private final SportWalletService sportWalletService;

    @Autowired
    public UnsettleService(HttpService httpService,
                           SportWalletService sportWalletService) {

        this.httpService = httpService;
        this.sportWalletService = sportWalletService;
    }

    public CommonVo unsettle(Action action, HttpRequestLog httpRequestLog) {
        String traceId = httpRequestLog.getId();
        Long transactionId = Optional.ofNullable(action.getTransaction()).map(ActionsTransactionDto::getTransactionId).orElse(null);
        Long wagerId = Optional.ofNullable(action.getWagerInfo()).map(ActionsWagerInfoDto::getWagerId).orElse(null);
        CommonVo commonVo = new CommonVo(action.getId(), transactionId, wagerId);

        try {
            UnsettleDto unsettleDto = new ModelMapper().map(action.getWagerInfo(), UnsettleDto.class);
            unsettleDto.setVendorPlayerUsername(action.getPlayerInfo().getUserCode());
            unsettleDto.setTransactionDate(Optional.ofNullable(action.getTransaction()).map(ActionsTransactionDto::getTransactionDate).orElse(null));
            unsettleDto.setExternalTransactionId(action.getId().toString());
            BetEvent response = sportWalletService.unsettle(traceId, unsettleDto, httpRequestLog.getRequestBody(), httpRequestLog);
            commonVo.setBalance(response.getLastBalance());

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            commonVo.setResponseCode(ResponseCode.INSUFFICIENT_FUND.code);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);

        }

        return commonVo;
    }
}
