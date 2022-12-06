package com.nextgen.gameaggregator.vendor.grpc.v1.listener;

import com.nextgen.gameaggregator.grpc.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.grpc.v1.vendor.betdetail.BetDetailGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.betdetail.BetDetailGrpcVo;
import com.nextgen.gameaggregator.grpc.v1.vendor.betdetail.BetDetailServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class VendorBetDetailGrpc extends BetDetailServiceGrpc.BetDetailServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(VendorBetDetailGrpc.class);
    private BetDetailGrpcVo vo;

    public void betDetail(final BetDetailGrpcDto dto,
                          final StreamObserver<BetDetailGrpcVo> responseObserver) {

        try {
            this.vo = BetDetailGrpcVo.newBuilder()
                    .setStatus(true)
                    .setDetailUrl("http://wwww.aaa.com")
                    .setVendorErrorCode(ConstantErrorMessage.SUCCESS_CODE)
                    .setVendorErrorMessage(ConstantErrorMessage.SUCCESS_MESSAGE)
                    .build();
        } catch (Exception exception) {
            logger.error(exception.getMessage());
            this.vo = BetDetailGrpcVo.newBuilder()
                    .setStatus(false)
                    .setDetailUrl("")
                    .setVendorErrorCode(ConstantErrorMessage.GAME_CLASS_PROCESSING_FAIL_CODE)
                    .setVendorErrorMessage(ConstantErrorMessage.GAME_CLASS_PROCESSING_FAIL)
                    .build();
        }
        responseObserver.onNext(this.vo);
        responseObserver.onCompleted();

    }

}
