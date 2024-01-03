package com.nextgen.gameaggregator.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.repository.VendorPlayerRepository;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.entity.SportUnsettledBetMariaDB;
import com.nextgen.gameaggregator.repository.UnsettledBetMariaDBRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "sportUnsettledBet/")
public class SportUnsettledBetController {
    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    RequestService requestService;
    @Autowired
    UnsettledBetMariaDBRepository unsettledBetMariaDBRepository;
    @Autowired
    VendorPlayerRepository vendorPlayerRepository;

    @PostMapping(path = "/details")
    public ResponseEntity<DetailVo> detail(@RequestBody ObjectNode json){
        DetailVo detailVo = new DetailVo();
        if (requestService.isTestEnvironment(profilesActive)) {
            VendorPlayer vendorPlayer = vendorPlayerRepository.findByUsername(json.get("username").asText());

            detailVo.setSportUnsettledBetMariaDB(
                    unsettledBetMariaDBRepository.findByExternalTransactionIdAndRoundIdAndVendorLineId(
                            json.get("externalTransactionId").asText(), json.get("roundId").asText(), vendorPlayer.getVendorLineId()));

            return new ResponseEntity<>(
                    detailVo,
                    HttpStatus.OK);
        }else{
            return new ResponseEntity<>(
                    detailVo,
                    HttpStatus.BAD_REQUEST);
        }

    }

    @Data
    static class DetailVo{

        public SportUnsettledBetMariaDB sportUnsettledBetMariaDB;
        public BetResultLog betResultLog;
        //public BetRefundLog betRefundLog;
    }
}
