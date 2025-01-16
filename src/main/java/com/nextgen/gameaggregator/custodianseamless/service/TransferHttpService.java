package com.nextgen.gameaggregator.custodianseamless.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionStatus;
import com.nextgen.gameaggregator.logging.TransferWalletRequestLog;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.service.HttpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.nextgen.gameaggregator.service.HttpService.THREAD_POOL;

@Service
@Slf4j
public class TransferHttpService {

    private final HttpService httpService;

    public TransferHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public TransferWalletRequestLog start(HttpServletRequest request) {
        TransferWalletRequestLog transferWalletRequestLog = new TransferWalletRequestLog();
        try {
            Map<String, String> headers = httpService.getHeadersInfo(request);
            String requestBody = httpService.getRawRequestBody(request);

            if (headers.containsKey("x-api-key")) {
                transferWalletRequestLog.setApiKey(request.getHeader("x-api-key"));
            }

            if (headers.containsKey("x-signature")) {
                transferWalletRequestLog.setSignature(request.getHeader("x-signature"));
            }

            transferWalletRequestLog.setRequestBody(requestBody);
            transferWalletRequestLog.setStatus(TransactionStatus.PROCESSING.status);

        } catch (Exception exception) {
            log.error(exception.getMessage());
        }

        return transferWalletRequestLog;
    }

    public void end(TransferWalletRequestLog transferWalletRequestLog, HttpResponse responseVo) {
        if (transferWalletRequestLog != null && responseVo != null) {
            transferWalletRequestLog.setEnd(System.currentTimeMillis());
            THREAD_POOL.submit(() -> {
                try {
                    String jsonResponseVo = new ObjectMapper().writeValueAsString(responseVo);
                    transferWalletRequestLog.setResponseBody(jsonResponseVo);
                    transferWalletRequestLog.setStatus(!responseVo.hasError() ?
                            TransactionStatus.SUCCESS.status : TransactionStatus.FAIL.status);

                    log.info(transferWalletRequestLog.toJson());

                } catch (Exception exception) {
                    log.error(exception.getMessage());
                }
            });

        } else {
            log.warn("HttpService.end: requestLog or responseVo is null");
        }
    }

    public void logError(TransferWalletRequestLog transferWalletRequestLog, Exception exception) {
        if (transferWalletRequestLog != null) {
            transferWalletRequestLog.setStatus(TransactionStatus.FAIL.status);
            transferWalletRequestLog.setException(exception.getClass().getSimpleName());
            transferWalletRequestLog.setExceptionMessage(exception.getMessage());
        } else {
            log.error("TransferWalletRequestLog is null: " + exception.getMessage());
        }
    }
}
