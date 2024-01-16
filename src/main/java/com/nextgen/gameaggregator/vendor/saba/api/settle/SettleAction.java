package com.nextgen.gameaggregator.vendor.saba.api.settle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.RawBatchProcessIdempotentLog;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class SettleAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private RawBatchProcessIdempotentLogService rawBatchProcessIdempotentLogService;
    @Autowired
    private SportWalletService sportWalletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.SETTLE)
    public GeneralVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        GeneralVo vo = new GeneralVo();

        try {
            // Convert original request body into dto
            RequestDto<SettleDto> dtos = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            String batchProcessId = vendorService.generateBatchProcessId(dtos.getMessage().getAction(), dtos.getMessage().getOperationId());
            if (rawBatchProcessIdempotentLogService.checkExists(batchProcessId) != null)
                throw new BetResultIdempotentViolationException();

            for (SettleBetTransactionDto txn : dtos.getMessage().getTxns()) {
                sportWalletService.asyncSettle(txn);
            }

            RawBatchProcessIdempotentLog rawBatchProcessIdempotentLog = new RawBatchProcessIdempotentLog(batchProcessId, dtos.getMessage().getAction(), httpRequestLog.getUrl());
            rawBatchProcessIdempotentLogService.create(rawBatchProcessIdempotentLog);

            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
//            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            vo.setResponseCode(ResponseCode.SUCCESS);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }
}
