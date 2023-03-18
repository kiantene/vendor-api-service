package com.nextgen.gameaggregator.vendor.jdb.api.helper;

import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class HelperAction {

    @Autowired
    private HttpService httpService;

    @PostMapping(path = "/decrypt")
    public String decrypt(@RequestBody Map<String, String> request) {
        String decryptedValue = "";
        try {
            decryptedValue = VendorService.decrypt(request.get("value"), "47e0cd2ece0883e2", "b87f2867577b68ce");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return decryptedValue;
    }
}
