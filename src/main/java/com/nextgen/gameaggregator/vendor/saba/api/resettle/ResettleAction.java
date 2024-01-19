package com.nextgen.gameaggregator.vendor.saba.api.resettle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RawBatchProcessIdempotentLogService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class ResettleAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private RawBatchProcessIdempotentLogService rawBatchProcessIdempotentLogService;
    @Autowired
    private SportWalletService sportWalletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.RESETTLE)
    public GeneralVo action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        GeneralVo vo = new GeneralVo();

        try {
            // Convert original request body into dto
            RequestDto<ResettleDto> dtos = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            String batchProcessId = vendorService.generateBatchProcessId(dtos.getMessage().getAction(), dtos.getMessage().getOperationId());
            if (rawBatchProcessIdempotentLogService.checkExists(batchProcessId) != null)
                throw new BetResultIdempotentViolationException();

            for (ResettleTransactionDto txn : dtos.getMessage().getTxns()) {
                sportWalletService.resettle(traceId, txn, httpRequestLog);
            }

            VendorGame.RawBatchProcessIdempotentLog rawBatchProcessIdempotentLog = new VendorGame.RawBatchProcessIdempotentLog(batchProcessId, dtos.getMessage().getAction(), httpRequestLog.getUrl());
            rawBatchProcessIdempotentLogService.create(rawBatchProcessIdempotentLog);

            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }
}
