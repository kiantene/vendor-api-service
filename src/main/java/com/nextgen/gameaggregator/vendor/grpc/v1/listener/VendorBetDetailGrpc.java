package com.nextgen.gameaggregator.vendor.grpc.v1.listener;

import com.nextgen.gameaggregator.grpc.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.grpc.v1.vendor.betdetail.BetDetailGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.betdetail.BetDetailGrpcVo;
import com.nextgen.gameaggregator.grpc.v1.vendor.betdetail.BetDetailServiceGrpc;
import com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.seamless.SeamlessVendorAdaptor;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class VendorBetDetailGrpc extends BetDetailServiceGrpc.BetDetailServiceImplBase {
    private SeamlessVendorAdaptor seamlessVendorAdaptor;

    public void betDetail(final BetDetailGrpcDto dto,
                          final StreamObserver<BetDetailGrpcVo> responseObserver) {

        BetDetailGrpcVo vo = BetDetailGrpcVo.newBuilder()
                .setStatus(true)
                .setDetailUrl("http://wwww.aaa.com")
                .setVendorErrorCode(ConstantErrorMessage.SUCCESS_CODE)
                .setVendorErrorMessage(ConstantErrorMessage.SUCCESS_MESSAGE)
                .build();

        responseObserver.onNext(vo);
        responseObserver.onCompleted();

    }

}
