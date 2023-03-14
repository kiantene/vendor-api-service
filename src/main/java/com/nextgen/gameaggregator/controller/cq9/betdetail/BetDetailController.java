package com.nextgen.gameaggregator.controller.cq9.betdetail;

import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetDetailController {

    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = "bet/status")
    public ResponseEntity<String> status(@RequestBody BetDetailDto dto) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.cqgame.games/gameboy/order/record?roundid={roundid}";
        Map<String, String> params = new HashMap<>();
//        params.put("gamecode", dto.getGameCode());
//        params.put("gamehall", Credentials.GAME_HALL);
        params.put("roundid", dto.getRoundId());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyaWQiOiI2M2FkMTQwYzc2MjA2OTYwMjQzMDg5YmQiLCJhY2NvdW50IjoienQwMDFjbnl0ZXN0Iiwib3duZXIiOiI2M2FjZjZkZjc2MjA2OTYwMjQzMDc3OGIiLCJwYXJlbnQiOiI2M2FjZjZkZjc2MjA2OTYwMjQzMDc3OGIiLCJjdXJyZW5jeSI6IkNOWSIsImp0aSI6IjU3NTI3ODgwOSIsImlhdCI6MTY3MjI4NzI0NCwiaXNzIjoiQ3lwcmVzcyIsInN1YiI6IlNTVG9rZW4ifQ.gbeQ-DZ2gb626JLC0XE2LjIchzmlxt-I_hX3y7UwUU0");
        headers.set("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class, params);
        if (response.getStatusCode() == HttpStatus.OK) {
            String result = response.getBody();
            // Do something with the result
        } else {
            // Handle error
        }

        return response;
    }
}
