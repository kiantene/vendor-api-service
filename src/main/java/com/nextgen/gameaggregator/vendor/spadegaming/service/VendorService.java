package com.nextgen.gameaggregator.vendor.spadegaming.service;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.spadegaming.api.transfer.WinDataDto;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Override
    public SettledBet updateSettleBetDataBeforeInsertToKafka(SettledBet settledBet, String rawData) {
        // Get the JSON request body from the HttpRequestLog
        String requestBody = rawData;
        Gson gson = new Gson();

        try {
            // Convert the JSON request body to WinDataDto object
            WinDataDto dto = gson.fromJson(requestBody, WinDataDto.class);

            // Remap vendorBetId with ticketId
            settledBet.setVendorBetId(dto.getTicketId());

        } catch (JsonParseException e) {
            log.error("Error parsing JSON: " + e.getMessage());
        }

        return settledBet;
    }
}
