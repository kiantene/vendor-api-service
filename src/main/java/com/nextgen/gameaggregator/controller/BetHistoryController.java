package com.nextgen.gameaggregator.controller;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.BetResultLog;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.BetHistoryRepository;
import com.nextgen.gameaggregator.repository.ga.writer.BetRefundLogRepository;
import com.nextgen.gameaggregator.repository.ga.writer.BetResultLogRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorPlayerRepository;
import com.nextgen.gameaggregator.service.RequestService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "betHistory/")
public class BetHistoryController {
    @Autowired
    RequestService requestService;
    @Autowired
    BetHistoryRepository betHistoryRepository;
    @Autowired
    BetResultLogRepository betResultLogRepository;
    @Autowired
    BetRefundLogRepository betRefundLogRepository;
    @Autowired
    VendorPlayerRepository vendorPlayerRepository;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    //for QA use to test get bet history detail
    @PostMapping(path = "/details")
    public ResponseEntity<DetailsVo> details(@RequestBody ObjectNode json) {
        DetailsVo detailsVo = new DetailsVo();
        if (requestService.isTestEnvironment(profilesActive)) {
            VendorPlayer vendorPlayer = vendorPlayerRepository.findByUsername(json.get("username").asText());

            List<BetHistory> betHistoryList = betHistoryRepository.findByExternalTransactionIdAndRoundIdAndVendorLineId(
                    json.get("externalTransactionId").asText(), json.get("roundId").asText(), vendorPlayer.getVendorLineId());

            List<BetHistory> sortedBetHistoryList = betHistoryList.stream()
                    .sorted(Comparator.comparingInt(BetHistory::getResettleNum))
                    .collect(Collectors.toList());

            detailsVo.setBetHistoryList(sortedBetHistoryList);

            return new ResponseEntity<>(
                    detailsVo,
                    HttpStatus.OK);
        } else {
            return new ResponseEntity<>(
                    detailsVo,
                    HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping(path = "/detail")
    public ResponseEntity<Detailvo> detail(@RequestBody ObjectNode json) {
        Detailvo detailvo = new Detailvo();
        if (requestService.isTestEnvironment(profilesActive)) {
            VendorPlayer vendorPlayer = vendorPlayerRepository.findByUsername(json.get("username").asText());

            List<BetHistory> betHistoryList = betHistoryRepository.findByExternalTransactionIdAndRoundIdAndVendorLineId(
                    json.get("externalTransactionId").asText(), json.get("roundId").asText(), vendorPlayer.getVendorLineId());

            BetHistory betHistory = betHistoryList.stream()
                    .max(Comparator.comparingInt(BetHistory::getResettleNum))
                    .orElse(null);

            detailvo.setBetHistory(betHistory);

            return new ResponseEntity<>(
                    detailvo,
                    HttpStatus.OK);
        } else {
            return new ResponseEntity<>(
                    detailvo,
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Data
    static class DetailsVo {

        public List<BetHistory> betHistoryList;
        public BetResultLog betResultLog;
        //public BetRefundLog betRefundLog;
    }

    @Data
    static class Detailvo {

        public BetHistory betHistory;
        public BetResultLog betResultLog;
        //public BetRefundLog betRefundLog;
    }
}

