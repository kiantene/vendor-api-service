package com.nextgen.gameaggregator.vendor.jili.api.bet;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.vendor.jili.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @PostMapping(path = EndPoints.BET)
    public BetVo BetAction (HttpServletRequest request) {

        HttpRequestLog log = httpService.start(request);
        BetVo vo = new BetVo();


        try{

            vo.setUsername("test001");
            vo.setCurrency("CNY");
            vo.setBalance(BigDecimal.valueOf(100));
            String hashText = this.md5(vo.getUsername());
            vo.setToken(hashText);


        }catch(Exception e){

        }finally{
            httpService.end(log, vo);
        }
        return vo;
    }

    private String md5(String input) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);

        while (hashtext.length() < 32)
        {
            hashtext = "0" + hashtext;
        }

        return hashtext;
    }
}
