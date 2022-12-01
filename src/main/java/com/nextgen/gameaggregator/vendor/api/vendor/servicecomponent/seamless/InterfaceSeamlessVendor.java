package com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.seamless;

import com.nextgen.gameaggregator.vendor.grpc.v1.dto.VendorGameLoginServiceRequestDto;
import com.nextgen.gameaggregator.vendor.grpc.v1.vo.VendorGameLoginServiceResponseVo;
import com.nextgen.gameaggregator.vendor.vendorclass.seamless.pragmaticplay.filter.LoggingFilter;
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
