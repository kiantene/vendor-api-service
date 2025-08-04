package com.nextgen.gameaggregator.vendor.aviatorstudio.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class AviatorStudioExceptionMapper implements VendorExceptionMapper<ResponseEntity<CommonVo>> {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public ResponseEntity<CommonVo> onInternalConfigurationError(InternalConfigurationException ex) {
        CommonVo responseVo = new CommonVo();
        responseVo.setResponseCode(ResponseCode.SERVER_ERROR);
        return new ResponseEntity<>(responseVo, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<CommonVo> onAuthenticationError(AuthenticationException ex) {
        CommonVo responseVo = new CommonVo();
        responseVo.setResponseCode(ResponseCode.AUTH_ERROR);
        return new ResponseEntity<>(responseVo, new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    @Override
    public ResponseEntity<CommonVo> onInsufficientBalance(InsufficientBalanceException ex) {
        CommonVo responseVo = new CommonVo();
        responseVo.setResponseCode(ResponseCode.INSUFFICIENT_FUNDS);
        return new ResponseEntity<>(responseVo, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<CommonVo> onInternalError(Throwable ex) {
        CommonVo responseVo = new CommonVo();
        responseVo.setResponseCode(ResponseCode.SERVER_ERROR);
        return new ResponseEntity<>(responseVo, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
