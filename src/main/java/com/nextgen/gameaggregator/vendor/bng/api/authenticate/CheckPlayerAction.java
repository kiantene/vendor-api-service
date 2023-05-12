package com.nextgen.gameaggregator.vendor.bng.api.authenticate;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bng.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bng.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bng.constant.Formats;
import com.nextgen.gameaggregator.vendor.bng.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bng.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.bng.vo.StatusVo;
import com.nextgen.gameaggregator.vendor.jili.api.authenticate.AuthVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CheckPlayerAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @PostMapping(path = EndPoints.ACTION)
    public CheckPlayerVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        CheckPlayerVo responseVo = new CheckPlayerVo();
        PlayerVo playerVo = new PlayerVo();
        playerVo.setId("42");
        playerVo.setBrand("iddqd");
        playerVo.setCurrency("CNY");
        playerVo.setMode("REAL");
        playerVo.setIs_test(false);
        BalanceVo balanceVo = new BalanceVo();
        balanceVo.setValue("16.20");
        balanceVo.setVersion("1");

        responseVo.setUid("5tz895f0e0ce11658ac8024eac110009");
        responseVo.setPlayerVo(playerVo);
        responseVo.setBalanceVo(balanceVo);
        responseVo.setTag("i_am_marking_this_session_because_i_can");

        httpService.end(httpRequestLog, responseVo);

        return responseVo;
    }
}
