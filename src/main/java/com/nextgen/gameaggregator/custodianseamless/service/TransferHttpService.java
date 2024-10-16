package com.nextgen.gameaggregator.custodianseamless.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionStatus;
import com.nextgen.gameaggregator.logging.TransferWalletRequestLog;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.KafkaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.nextgen.gameaggregator.service.HttpService.THREAD_POOL;

@Service
@Slf4j
public class TransferHttpService {

    @Value("${version}")
    private String appVersion;
    private final HttpService httpService;
    private final KafkaService kafkaService;

    public TransferHttpService(HttpService httpService, KafkaService kafkaService) {
        this.httpService = httpService;
        this.kafkaService = kafkaService;
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

            transferWalletRequestLog.setVer(this.appVersion);
            transferWalletRequestLog.setServer(request.getLocalName());
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
                    String jsonResponseVo = new Gson().toJson(responseVo);
                    transferWalletRequestLog.setResponseBody(jsonResponseVo);
                    transferWalletRequestLog.setStatus(!responseVo.hasError() ?
                            TransactionStatus.SUCCESS.status : TransactionStatus.FAIL.status);

                    kafkaService.produceTransferWalletRequestLog(transferWalletRequestLog);

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
