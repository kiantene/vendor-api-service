package com.nextgen.gameaggregator.vendor.component.vendor;

import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcVo;
import com.nextgen.gameaggregator.vendor.exception.VendorApiException;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;

@Component
public interface InterfaceSeamlessVendor {
    public GameLoginGrpcVo gameLogin(GameLoginGrpcDto dto);
    public GameLoginGrpcVo verifyGameLoginResponse(String response)  throws VendorApiException;

    public String vendorAPICall(MultiValueMap<String, String> paramMap, String endPoint) throws VendorApiException;

    public HashMap<String, Object> gameAuthentication(HashMap<String, Object> map);

    public HashMap<String, Object> walletBalance(HashMap<String, Object> map);

    public HashMap<String, Object> betRequest(HashMap<String, Object> map);

    public HashMap<String, Object> betResult(HashMap<String, Object> map);

    public HashMap<String, Object> endRound(HashMap<String, Object> map);

    public HashMap<String, Object> refund(HashMap<String, Object> map);

    public HashMap<String, Object> bonusWin(HashMap<String, Object> map);

    public HashMap<String, Object> jackpotWin(HashMap<String, Object> map);

    public HashMap<String, Object> promoWin(HashMap<String, Object> map);

}
