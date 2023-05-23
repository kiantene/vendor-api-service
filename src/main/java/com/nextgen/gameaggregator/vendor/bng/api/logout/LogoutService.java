package com.nextgen.gameaggregator.vendor.bng.api.logout;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bng.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bng.constant.Credentials;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogoutService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;

    public CommonVo logout(HttpRequestLog httpRequestLog, String traceId) {

        // Construct VO
        LogoutVo vo = new LogoutVo();

        try{

            LogoutDto logoutDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), LogoutDto.class);

            vo.setUid(logoutDto.getUid());

        }catch(Exception exception){

        }

        return vo;
    }
}
