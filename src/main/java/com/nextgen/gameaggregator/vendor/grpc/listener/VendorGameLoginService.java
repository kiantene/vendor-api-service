package com.nextgen.gameaggregator.vendor.grpc.listener;

import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.*;
import com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.seamless.SeamlessVendorAdaptor;
import com.nextgen.gameaggregator.vendor.grpc.dto.VendorGameLoginServiceRequestDto;
import com.nextgen.gameaggregator.vendor.grpc.vo.VendorGameLoginServiceResponseVo;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class VendorGameLoginService extends GameLoginServiceGrpc.GameLoginServiceImplBase {

    private SeamlessVendorAdaptor  seamlessVendorAdaptor;

    @Autowired
    private VendorReaderManager vendorReaderManager;
    private VendorReader vendorReader;

    private String vendorPlayerUsername = null;
    private String playerSessionId = null;

    public VendorGameLoginService(SeamlessVendorAdaptor seamlessVendorAdaptor) {
        this.seamlessVendorAdaptor = seamlessVendorAdaptor;
    }

    public void gameLogin(final GameLoginDto dto, final StreamObserver<GameLoginVo> responseObserver) {

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

        GameLoginVo vo = GameLoginVo.newBuilder()
                .setStatus(responseVo.getStatus())
                .setGameUrl(responseVo.getGameUrl())
                .setVendorErrorCode(responseVo.getVendorErrorCode())
                .setVendorErrorMessage(responseVo.getVendorErrorMessage())
                .build();

        responseObserver.onNext(vo);
        responseObserver.onCompleted();
    }

}
