package com.nextgen.gameaggregator.vendor.component.vendor;

import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcVo;
import com.nextgen.gameaggregator.vendor.exception.VendorApiException;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

@Component
public interface InterfaceSeamlessVendor {
    public GameLoginGrpcVo gameLogin(GameLoginGrpcDto dto);
    public GameLoginGrpcVo verifyGameLoginResponse(String response)  throws VendorApiException;

    public String vendorAPICall(MultiValueMap<String, String> paramMap, String endPoint) throws VendorApiException;

}
