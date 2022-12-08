package com.nextgen.gameaggregator.vendor.grpc.v1.listener;

import com.nextgen.gameaggregator.grpc.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcVo;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginServiceGrpc;
import com.nextgen.gameaggregator.vendor.component.vendor.VendorAdaptor;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

@GrpcService
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class VendorGameLoginGrpc extends GameLoginServiceGrpc.GameLoginServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(VendorGameLoginGrpc.class);


    @Autowired
    private VendorAdaptor vendorAdaptor;

    public void gameLogin(final GameLoginGrpcDto dto, final StreamObserver<GameLoginGrpcVo> responseObserver) {

        GameLoginGrpcVo vo;
        try {
            if (vendorAdaptor.getVendor(dto.getVendorId(), dto.getWalletType(), dto.getVendorCredentialId())) {
                if(dto.getWalletType() == 1){
                    vo = vendorAdaptor.seamlessVendor.gameLogin(dto);
                }else{
                    vo = vendorAdaptor.transferVendor.gameLogin(dto);
                }
            } else {
                vo = GameLoginGrpcVo.newBuilder()
                        .setStatus(false)
                        .setGameUrl("")
                        .setVendorErrorCode(ConstantErrorMessage.GAME_CLASS_NOT_IMPLEMENT_CODE)
                        .setVendorErrorMessage(ConstantErrorMessage.GAME_CLASS_NOT_IMPLEMENT)
                        .build();
            }
        } catch (Exception exception) {
            logger.error(exception.getMessage());
            vo = GameLoginGrpcVo.newBuilder()
                    .setStatus(false)
                    .setGameUrl("")
                    .setVendorErrorCode(ConstantErrorMessage.GAME_CLASS_PROCESSING_FAIL_CODE)
                    .setVendorErrorMessage(ConstantErrorMessage.GAME_CLASS_PROCESSING_FAIL)
                    .build();

        }
        responseObserver.onNext(vo);
        responseObserver.onCompleted();
    }

}
