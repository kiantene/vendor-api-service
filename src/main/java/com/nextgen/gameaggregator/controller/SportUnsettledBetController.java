package com.nextgen.gameaggregator.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.ga.BetResultLog;
import com.nextgen.gameaggregator.entity.ga.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.UnsettledBetMariaDBRepository;
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
@RequestMapping(path = "sportUnsettledBet/")
public class SportUnsettledBetController {
    @Autowired
    RequestService requestService;
    @Autowired
    UnsettledBetMariaDBRepository unsettledBetMariaDBRepository;
    @Autowired
    VendorPlayerRepository vendorPlayerRepository;

    @PostMapping(path = "/details")
    public ResponseEntity<DetailVo> detail(@RequestBody ObjectNode json) {
        DetailVo detailVo = new DetailVo();
        if (requestService.isTestEnvironment()) {
            VendorPlayer vendorPlayer = vendorPlayerRepository.findByUsername(json.get("username").asText());

            List<SportUnsettledBetMariaDB> sportUnsettledBetMariaDBList = unsettledBetMariaDBRepository.findByExternalTransactionIdAndRoundIdAndVendorLineId(
                    json.get("externalTransactionId").asText(), json.get("roundId").asText(), vendorPlayer.getVendorLineId());

            List<SportUnsettledBetMariaDB> sortedSportUnsettledBetMariaDBList = sportUnsettledBetMariaDBList.stream()
                    .sorted(Comparator.comparingLong(SportUnsettledBetMariaDB::getCreateDate))
                    .collect(Collectors.toList());

            detailVo.setSportUnsettledBetMariaDB(sortedSportUnsettledBetMariaDBList);

            return new ResponseEntity<>(
                    detailVo,
                    HttpStatus.OK);
        } else {
            return new ResponseEntity<>(
                    detailVo,
                    HttpStatus.BAD_REQUEST);
        }

    }

    @Data
    static class DetailVo {

        public List<SportUnsettledBetMariaDB> sportUnsettledBetMariaDB;
        public BetResultLog betResultLog;
        //public BetRefundLog betRefundLog;
    }
}
