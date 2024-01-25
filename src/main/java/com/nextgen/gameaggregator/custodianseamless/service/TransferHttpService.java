package com.nextgen.gameaggregator.custodianseamless.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionStatus;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TransferWalletRequestLog;
import com.nextgen.gameaggregator.service.HttpResponse;
import com.nextgen.gameaggregator.service.HttpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.nextgen.gameaggregator.service.HttpService.THREAD_POOL;

@Service
@Slf4j
public class TransferHttpService {

    @Value("${logging.http-request:true}")
    private Boolean enableHttpRequestLog;

    @Autowired
    private HttpService httpService;

    public TransferWalletRequestLog start(HttpServletRequest request) {
        TransferWalletRequestLog transferWalletRequestLog = new TransferWalletRequestLog();
        try {
            Map<String, String> headers = httpService.getHeadersInfo(request);
            String requestBody = httpService.getRawRequestBody(request);

            if (headers.containsKey("host")) {
                transferWalletRequestLog.setHost(headers.get("host"));
            }

            if (headers.containsKey("x-api-key")) {
                transferWalletRequestLog.setApiKey(request.getHeader("x-api-key"));
            }

            if (headers.containsKey("x-signature")) {
                transferWalletRequestLog.setSignature(request.getHeader("x-signature"));
            }

            if (headers.containsKey("cf-connecting-ip")) {
                transferWalletRequestLog.setCallerIp(request.getHeader("cf-connecting-ip"));
            }

            if (headers.containsKey("user-agent")) {
                transferWalletRequestLog.setUserAgent(request.getHeader("user-agent"));
            }

            transferWalletRequestLog.setUrl(request.getRequestURI());
            transferWalletRequestLog.setMethod(request.getMethod());
            transferWalletRequestLog.setRequestBody(requestBody);
            transferWalletRequestLog.setStatus(TransactionStatus.PROCESSING.status);
            transferWalletRequestLog.setRequestIp(request.getRemoteAddr());
            transferWalletRequestLog.setStartTime(System.currentTimeMillis());
            String jsonHeaders = new Gson().toJson(headers.toString());
            transferWalletRequestLog.setHeader(jsonHeaders);

        } catch (Exception exception) {
            log.error(exception.getMessage());
            exception.printStackTrace();
        }

        return transferWalletRequestLog;
    }

    public void end(TransferWalletRequestLog transferWalletRequestLog, HttpResponse responseVo) {
        if (!enableHttpRequestLog) return;

        if (transferWalletRequestLog != null && responseVo != null) {
            transferWalletRequestLog.setEndTime(System.currentTimeMillis());
            THREAD_POOL.submit(() -> {

            });

        }else {
            log.warn("HttpService.end: requestLog or responseVo is null");
        }
    }
}
