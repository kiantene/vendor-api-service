package com.nextgen.gameaggregator.vendor.api.pgsoft.service.v2_4_4;

import com.nextgen.gameaggregator.grpc.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.vendor.gamelogin.GameLoginGrpcVo;
import com.nextgen.gameaggregator.vendor.component.vendor.AbstractVendor;
import com.nextgen.gameaggregator.vendor.component.vendor.InterfaceSeamlessVendor;
import com.nextgen.gameaggregator.vendor.exception.VendorApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.HashMap;

@Component("seamless_pp_v2.4.4")
public class Seamless_PP_v2_4_4 extends AbstractVendor implements InterfaceSeamlessVendor {

    private static final Logger logger = LoggerFactory.getLogger(Seamless_PP_v2_4_4.class);

    public Seamless_PP_v2_4_4() {
    }

    public Seamless_PP_v2_4_4(Long vendorId, Long vendorCredentialId) {
        this.setVendorIdAndCredentialId(vendorId, vendorCredentialId);
    }

    //region Game Login
    @Override
    public GameLoginGrpcVo gameLogin(GameLoginGrpcDto dto) {

        try {
            String openGameCode = this.findVendorGameCode(dto.getGameId(), dto.getLanguage(), dto.getPlatform());

            this.setCredential();
            this.findVendorPlayerUsername(dto.getAgentPlayerId(), dto.getAgentId(), dto.getMasterAgentId(), dto.getHouseId(), dto.getCurrency(),true);
            this.createPlayerAuthentication(
                dto.getWalletType(),
                dto.getAgentPlayerId(),
                this.vendorPlayerReader.getId(),
                this.vendorPlayerReader.getVendorUsername(),dto.getPlatform(),
                this.findVendorPlatformCode(dto.getPlatform()),
                dto.getLanguage(),
                this.findVendorLanguageCode(dto.getLanguage()),
                dto.getGameId(),
                openGameCode,
                dto.getAgentId(),
                dto.getTraceId(),
                dto.getCurrency(),
                this.findVendorCurrencyCode(dto.getCurrency(), dto.getVendorId()),
                Instant.now().toEpochMilli(),
                dto.getVendorCredentialId()
            );

            return GameLoginGrpcVo.newBuilder()
                    .setStatus(true)
                    .setGameUrl(this.constructGameUrl(openGameCode))
                    .setVendorErrorCode(ConstantErrorMessage.SUCCESS_CODE)
                    .setVendorErrorMessage(ConstantErrorMessage.SUCCESS_MESSAGE)
                    .build();

        } catch (Exception exception) {
            logger.error(exception.getMessage());
            return  GameLoginGrpcVo.newBuilder()
                    .setStatus(false).setGameUrl("")
                    .setVendorErrorCode(ConstantErrorMessage.UNEXPECTED_ERROR_CODE)
                    .setVendorErrorMessage(ConstantErrorMessage.UNEXPECTED_ERROR)
                    .build();
        }
    }

    @Override
    public GameLoginGrpcVo verifyGameLoginResponse(String response) throws VendorApiException {
        return null;
    }

    @Override
    public String vendorAPICall(MultiValueMap<String, String> paramMap, String endPoint) throws VendorApiException {
        return null;
    }

    @Override
    public HashMap<String, Object> gameAuthentication(HashMap<String, Object> map) {
        return null;
    }

    @Override
    public HashMap<String, Object> walletBalance(HashMap<String, Object> map) {
        return null;
    }

    @Override
    public HashMap<String, Object> betRequest(HashMap<String, Object> map) {
        return null;
    }

    @Override
    public HashMap<String, Object> betResult(HashMap<String, Object> map) {
        return null;
    }

    @Override
    public HashMap<String, Object> endRound(HashMap<String, Object> map) {
        return null;
    }

    @Override
    public HashMap<String, Object> refund(HashMap<String, Object> map) {
        return null;
    }

    @Override
    public HashMap<String, Object> bonusWin(HashMap<String, Object> map) {
        return null;
    }

    @Override
    public HashMap<String, Object> jackpotWin(HashMap<String, Object> map) {
        return null;
    }

    private String constructGameUrl(String openGameCode) {
        return credentialMap.get("publicUrl") + "/" + openGameCode + "/index.html";
    }

}
