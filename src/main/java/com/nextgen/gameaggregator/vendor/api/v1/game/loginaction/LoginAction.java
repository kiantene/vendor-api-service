package com.nextgen.gameaggregator.vendor.api.v1.game.loginaction;

import com.nextgen.gameaggregator.vendor.api.v1.apicomponent.log.TraceIdLog;
import com.nextgen.gameaggregator.vendor.grpc.v1.dto.VendorGameAuthenticationServiceRequestDto;
import com.nextgen.gameaggregator.vendor.grpc.v1.vo.VendorGameAuthenticationServiceResponseVo;
import com.nextgen.gameaggregator.vendor.util.NameUtils;
import com.nextgen.sas.core.web.wrapper.WebRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping(path = "api/v1/prammaticplay/", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
public class LoginAction {

    private static final Logger logger = LoggerFactory.getLogger(LoginAction.class);

    @Autowired
    private TraceIdLog traceIdLog;



    @PostMapping(path = "testing2" )
    public VendorGameAuthenticationServiceResponseVo authenticate(VendorGameAuthenticationServiceRequestDto dto,
                                                                  WebRequestWrapper request) throws IOException {

        Map<String,Object> map=new HashMap<String,Object>();
        HashMap<String, String> output = new HashMap<String, String>();

        //output = this.handleQueryStringDataToMapObject(request.getBody());



        //WebRequestWrapper webRequestWrapper = new WebRequestWrapper(request);
        System.out.println("test action print getMethod::: "+request.getMethod());
        System.out.println("test action print getRequestURI::: "+request.getRequestURI());
        System.out.println("test action print body::: "+request.getBody());
        System.out.println("test action print getContentType::: "+request.getContentType());
        System.out.println("test action print getParameterMap::: "+request.getParameterMap());
        System.out.println("test action print getRemoteAddr::: "+request.getRemoteAddr());
        System.out.println(new StringBuilder().append("test action print getReader::: ").append(request.getReader()).toString());

        System.out.println("========== Base58 Encoding Long data type ==========");
        String username = NameUtils.generateUsername("0", 1L, 1L, 1L);
        System.out.println("username::: "+username);

        map.putAll(request.getParameterMap());
        System.out.println("map::: "+map.get("hash"));

//        map.put("userId", "33333");
//        map.put("currency", "CNY");
//        map.put("cash", 100.00);
//        map.put("bonus", "0");
//        map.put("error", 0);
//        map.put("description", "Success");



        return new VendorGameAuthenticationServiceResponseVo("33333","CNY", 100.00d,0.00d,
                "f8c3de3d-1fea-4d7c-a8b0-29f63c4c3457", 0,"Success");
//        return new ResponseVo<Map<String,Object>>("Success", map);
    }

}
