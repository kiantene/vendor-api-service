package com.nextgen.gameaggregator.vendor.api.pragmaticplay.service;

import com.nextgen.gameaggregator.grpc.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcVo;
import com.nextgen.gameaggregator.vendor.component.vendor.InterfaceTransferVendor;
import org.springframework.stereotype.Component;

@Component("transfer_pragmaticplayv3.180")
public class TransferPragmaticPlayV3_180 implements InterfaceTransferVendor {

    @Override
    public GameLoginGrpcVo gameLogin(GameLoginGrpcDto dto) {
        return GameLoginGrpcVo.newBuilder()
                .setStatus(true)
                .setGameUrl("http://www.transfer.pragmaticplay.com")
                .setVendorErrorCode(ConstantErrorMessage.SUCCESS_CODE)
                .setVendorErrorMessage(ConstantErrorMessage.SUCCESS_MESSAGE)
                .build();
    }
}
