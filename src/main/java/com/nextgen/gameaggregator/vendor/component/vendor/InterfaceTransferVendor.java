package com.nextgen.gameaggregator.vendor.component.vendor;

import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcVo;

public interface InterfaceTransferVendor {

    public GameLoginGrpcVo gameLogin(GameLoginGrpcDto dto);

}
