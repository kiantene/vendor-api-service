package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.authenticate;

import com.nextgen.gameaggregator.grpc.v1.operator.walletbalance.WalletBalanceGrpcVo;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.action.AbstractAction;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.VendorPlayerAuthentication;
import com.nextgen.gameaggregator.vendor.grpc.v1.subcriber.OperatorWalletBalanceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.Constant.VENDOR_CODE;

@RestController
@RequestMapping(path = Constant.WEB_ACTION, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
public class AuthenticationAction extends AbstractAction {

    @Autowired
    HttpServletRequest req;

    @Autowired
    private OperatorWalletBalanceGrpc operatorWalletBalanceGrpc;

    @PostMapping(path = Constant.ACTION_AUTHENTICATE)
    public AuthenticationVo gameAuthenticate(AuthenticationDto dto) throws IOException {
        AuthenticationVo thisVo = new AuthenticationVo();

        String body = req.getReader().lines().collect(Collectors.joining());


        System.out.println(body);
        System.out.println(req.getContentLength());
        String traceId = UUID.randomUUID().toString();
        String grpcError = null;
        String grpcErrorDes = null;

        //* Temporary solution to map into DTO
        dto = this.queryStringToDto(body, AuthenticationDto.class);

        thisVo.setTraceId(traceId);

        //* DTO Validation
        Map<String, String> dtoValidationResult = this.doValidation(dto, AuthenticationDto.class);
        //* Verify validation result
        thisVo.verifyValidationResultAndManipulateErrorAndDescription(dtoValidationResult);

        String test = "";
        for (Map.Entry<String, String> entry : dtoValidationResult.entrySet()) {
            test += entry.getKey() + "/" + entry.getValue();
        }
        test += "| body :"+ body;
        thisVo.setErrorCheck(test);

        //region create result log for all request that comes to result end point
        Long aggregatorRequestStartMs = Instant.now().toEpochMilli();
        String playerToken = (dto.getToken() == null)?"_NULL": "_"+dto.getToken();
        this.createSeamlessResultLogRecord(VENDOR_CODE+"_authentication_"+aggregatorRequestStartMs+playerToken, aggregatorRequestStartMs,
                body);
        //endregion

        //region if verify validation result is clean
        if (thisVo.getError() == ConstantErrorMessage.CODE_SUCCESS) {
            VendorPlayerAuthentication vendorPlayerAuthentication;
            vendorPlayerAuthentication = this.findTraceId(dto.getToken());

            //check is player token exists
            if (vendorPlayerAuthentication != null) {
                //default set as error, and require vendor to resend request
                thisVo.setErrorAndDescriptionByConstantResponseKey(ConstantErrorMessage.RESPONSE_KEY_INTERNAL_SERVER_ERROR_RECONCILIATION);

                try{
                    thisVo.setUserId(vendorPlayerAuthentication.getVendorPlayerUsername());
                    thisVo.setCurrency(vendorPlayerAuthentication.getCurrencyCode());
                    thisVo.setBonus(BigDecimal.valueOf(0d));
                    thisVo.setToken(vendorPlayerAuthentication.getTraceId());

                    //prepare grpc info and send over to grpc
                    WalletBalanceGrpcVo serviceVo = this.operatorWalletBalanceGrpc.walletBalance(
                            vendorPlayerAuthentication.getAgentId(),
                            vendorPlayerAuthentication.getAgentPlayerId(),
                            vendorPlayerAuthentication.getVendorId(),
                            vendorPlayerAuthentication.getCurrencyCode(),
                            traceId,
                            this.findAgentCredentialIdByAgentId(vendorPlayerAuthentication.getAgentId()));

                    if(serviceVo.getStatus()){
                        //if grpc success, set as success and set the responding balance amount
                        thisVo.setErrorAndDescriptionByConstantResponseKey(ConstantErrorMessage.RESPONSE_KEY_SUCCESS);
                        thisVo.setCash((BigDecimal.valueOf(serviceVo.getBalance())).setScale(2, RoundingMode.HALF_DOWN));
                    }else{
                        thisVo.setErrorAndDescriptionByConstantResponseKey(ConstantErrorMessage.RESPONSE_KEY_INTERNAL_SERVER_ERROR_RECONCILIATION);
                        grpcError = serviceVo.getOperatorErrorCode();
                        grpcErrorDes = serviceVo.getOperatorErrorMessage();
                    }
                } catch (ClassCastException e){
                    //try catch for any class cast error, return error for vendor and require to resend request
                }
            } else {
                thisVo.setErrorAndDescriptionByConstantResponseKey(ConstantErrorMessage.RESPONSE_KEY_PLAYER_AUTH_FAILED);
            }
        }
        //endregion

        System.out.println("authentication traceId ::::::" + traceId);
        System.out.println("authentication thisVo ::::::" + thisVo);

        if (thisVo.getError() != 0){
            //region create ERROR log for all request that if error not = 0
            Long aggregatorRequestStartMsErr = Instant.now().toEpochMilli();
            String playerTokenErr = (dto.getToken() == null)?"_NULL": "_"+dto.getToken();
            this.createSeamlessResultLogRecord(VENDOR_CODE+"_authenticationErr_"+aggregatorRequestStartMsErr+playerTokenErr, aggregatorRequestStartMsErr,
                    test+"||"+thisVo+"||"+grpcError+"||"+grpcErrorDes);
            //endregion
        }

        return thisVo;
    }
}
