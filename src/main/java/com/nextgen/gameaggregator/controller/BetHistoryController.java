package com.nextgen.gameaggregator.controller;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.BetRefundLogRepository;
import com.nextgen.gameaggregator.repository.BetResultLogRepository;
import com.nextgen.gameaggregator.repository.VendorPlayerRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "betHistory/")
public class BetHistoryController {

    @Autowired
    BetHistoryRepository betHistoryRepository;
    @Autowired
    BetResultLogRepository betResultLogRepository;
    @Autowired
    BetRefundLogRepository betRefundLogRepository;
    @Autowired
    VendorPlayerRepository vendorPlayerRepository;

    //for QA use to test get bet history detail
    @PostMapping(path = "/detail")
    public ResponseEntity<Detailvo> detail(@RequestBody ObjectNode json){
        Detailvo detailvo = new Detailvo();
        VendorPlayer vendorPlayer = vendorPlayerRepository.findByUsername(json.get("username").asText());

        detailvo.setBetHistory(
                betHistoryRepository.findByExternalTransactionIdAndRoundIdAndVendorLineId(
                        json.get("externalTransactionId").asText(), json.get("roundId").asText(), vendorPlayer.getVendorLineId()));

//        detailvo.setBetResultLog(betResultLogRepository.findByExternalTransactionIdAndRoundIdAndVendorLineId(
//                json.get("externalTransactionId").asText(), json.get("roundId").asText(), vendorPlayer.getVendorLineId()));
//
//        detailvo.setBetRefundLog(betRefundLogRepository.findByExternalTransactionIdAndRoundIdAndVendorPlayerId(
//                json.get("externalTransactionId").asText(), json.get("roundId").asText(), vendorPlayer.getVendorLineId()));

        return new ResponseEntity<>(
                detailvo ,
                HttpStatus.OK);

    }

    @Data
     static class Detailvo{

        public BetHistory betHistory;
        public BetResultLog betResultLog;
        //public BetRefundLog betRefundLog;
    }
}

