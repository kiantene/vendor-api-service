package com.nextgen.gameaggregator.vendor.spribe.service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.spribe.api.result.SettleDto;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    private final HttpService httpService;

    private final WalletService walletService;

    @Autowired
    public VendorService(HttpService httpService, WalletService walletService) {
        this.httpService = httpService;
        this.walletService = walletService;
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }

    public String toQueryString(MultiValueMap<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
                queryString.append("=");
                queryString.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return queryString.toString();
    }

    @Override
    public SettledBet updateSettleBetDataBeforeInsertToKafka(SettledBet settledBet, String rawData) {
        // Get the JSON request body from the HttpRequestLog
        String requestBody = rawData;
        Gson gson = new Gson();

        try {
            // Convert the JSON request body to WinDataDto object
            SettleDto dto = gson.fromJson(requestBody, SettleDto.class);

            // Remap vendorBetId with withdraw provide tx id
            if (StringUtils.isNotBlank(dto.getWithdraw_provider_tx_id())) {
                settledBet.setVendorBetId(dto.getWithdraw_provider_tx_id());
            }

        } catch (JsonParseException e) {
            log.error("Error parsing JSON: " + e.getMessage());
        }
        return settledBet;
    }
}