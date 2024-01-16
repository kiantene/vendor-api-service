package com.nextgen.gameaggregator.vendor.saba.api.cancelbet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetRefundIdempotentViolationException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class CancelBetAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;

    @PostMapping(path = EndPoints.CANCEL_BET)
    public GeneralVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        GeneralVo vo = new GeneralVo();

        try {
            // Convert original request body into dto
            RequestDto<CancelBetDto> dtos = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            vo.setResponseCode(ResponseCode.SUCCESS);

            for (CancelBetTransactionDto txn : dtos.getMessage().getTxns()) {
                dtos.getMessage().setRefId(txn.getRefId());
                GeneralVo response = this.singleRefund(dtos.getMessage(), httpRequestLog);

                if (vo.getStatus().equals(ResponseCode.SUCCESS.status) && response.getStatus().equals(ResponseCode.DUPLICATE_TRANSACTION.status)) {
                    vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);
                } else if (vo.getStatus().equals(ResponseCode.SUCCESS.status) && response.getStatus().equals(ResponseCode.NO_SUCH_TICKET_CANCEL_BET_RETRY.status)) {
                    vo.setResponseCode(ResponseCode.NO_SUCH_TICKET_CANCEL_BET_RETRY);
                } else if (vo.getStatus().equals(ResponseCode.SUCCESS.status) && response.getStatus().equals(ResponseCode.SYSTEM_ERROR_RETRY.status)) {
                    vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
                }
            }

        } catch (
                Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }

    private GeneralVo singleRefund(CancelBetDto txn, HttpRequestLog httpRequestLog) {
        // Construct Vo
        GeneralVo vo = new GeneralVo();
        BetEvent betEvent = null;

        try {
            String traceId = UUID.randomUUID().toString();
            betEvent = sportWalletService.refund(traceId, txn, httpRequestLog.getRequestBody(), httpRequestLog);

            vo.setResponseCode(ResponseCode.SUCCESS);
            vo.setBalance(betEvent == null ? BigDecimal.ZERO : betEvent.getLastBalance());

        } catch (BetNotFoundException e) {
            vo.setResponseCode(ResponseCode.NO_SUCH_TICKET_CANCEL_BET_RETRY);
            httpService.logError(httpRequestLog, e);

        } catch (BetRefundIdempotentViolationException e) {
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);

        }

        return vo;
    }
}
