package com.nextgen.gameaggregator.vendorapiservice.api.vendor.servicecomponent.seamless;

import com.nextgen.gameaggregator.vendorapiservice.grpc.dto.VendorGameAuthenticationServiceRequestDto;
import com.nextgen.gameaggregator.vendorapiservice.grpc.dto.VendorGameLoginServiceRequestDto;
import com.nextgen.gameaggregator.vendorapiservice.grpc.vo.VendorGameAuthenticationServiceResponseVo;
import com.nextgen.gameaggregator.vendorapiservice.grpc.vo.VendorGameLoginServiceResponseVo;
import com.nextgen.gameaggregator.vendorapiservice.vendorclass.seamless.pragmaticplay.filter.LoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public interface InterfaceSeamlessVendor {

    static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);
    VendorGameLoginServiceResponseVo vendorGameLoginServiceResponseVo = new VendorGameLoginServiceResponseVo();

    public VendorGameLoginServiceResponseVo gameLogin(VendorGameLoginServiceRequestDto dto);

    //public VendorGameAuthenticationServiceResponseVo gameAuthentication(VendorGameAuthenticationServiceRequestDto dto);
    public HashMap<String, Object> gameAuthentication(HashMap<String, Object> map);

    public String gameLogout();
    public HashMap<String, Object> walletBalance(HashMap<String, Object> map);

    public HashMap<String, Object> betRequest(HashMap<String, Object> map);

    public HashMap<String, Object> betResult(HashMap<String, Object> map);

//    public String deposit();
//    public String withdraw();
}
