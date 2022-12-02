package com.nextgen.gameaggregator.vendor.grpc.v1.listener;

import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcVo;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginServiceGrpc;
import com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.seamless.SeamlessVendorAdaptor;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorReaderManager;
import com.nextgen.gameaggregator.vendor.grpc.v1.dto.VendorGameLoginServiceRequestDto;
import com.nextgen.gameaggregator.vendor.grpc.v1.vo.VendorGameLoginServiceResponseVo;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class VendorGameLoginGrpc extends GameLoginServiceGrpc.GameLoginServiceImplBase {

    private SeamlessVendorAdaptor  seamlessVendorAdaptor;

    @Autowired
    private VendorReaderManager vendorReaderManager;
    private VendorReader vendorReader;

    private String vendorPlayerUsername = null;
    private String playerSessionId = null;

    public VendorGameLoginGrpc(SeamlessVendorAdaptor seamlessVendorAdaptor) {
        this.seamlessVendorAdaptor = seamlessVendorAdaptor;
    }

    public void gameLogin(final GameLoginGrpcDto dto, final StreamObserver<GameLoginGrpcVo> responseObserver) {

        //merge data from VendorGameLoginServiceDto to VendorGameLoginServiceRequestDto for class file processing
        VendorGameLoginServiceRequestDto requestDto = new VendorGameLoginServiceRequestDto(
                dto.getAgentPlayerId(),
                dto.getVendorCredentialId(),
                dto.getVendorId(),
                dto.getGameId(),
                dto.getLanguage(),
                dto.getPlatform(),
                dto.getCurrency(),
                dto.getAgentId(),
                dto.getPlayerUsername(),
                dto.getHouseId(),
                dto.getMasterAgentId(),
                dto.getTraceId(),
                dto.getWalletType()
        );

        //region get vendor_class file name
        vendorReader = vendorReaderManager.findById(dto.getVendorId()).orElse(null);
        //endregion

        //region call to correct class file
        seamlessVendorAdaptor.getVendor(vendorReader.getClassFile());
        //endregion

        VendorGameLoginServiceResponseVo responseVo = seamlessVendorAdaptor.seamlessVendor.gameLogin(requestDto);

        GameLoginGrpcVo vo = GameLoginGrpcVo.newBuilder()
                .setStatus(responseVo.getStatus())
                .setGameUrl(responseVo.getGameUrl())
                .setVendorErrorCode(responseVo.getVendorErrorCode())
                .setVendorErrorMessage(responseVo.getVendorErrorMessage())
                .build();

        responseObserver.onNext(vo);
        responseObserver.onCompleted();
    }

}
