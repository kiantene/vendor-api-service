package com.nextgen.gameaggregator.vendor.pgsoft.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutVo;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import org.springframework.stereotype.Component;

@Component
public class PGSoftExceptionMapper implements VendorExceptionMapper<ResponseVo<CashTransferInOutVo>> {

    @Override
    public String getVendorClassName() {
        return Endpoints.CLASS_NAME;
    }

    @Override
    public ResponseVo<CashTransferInOutVo> onInternalConfigurationError(InternalConfigurationException ex) {

        return null;
    }

    @Override
    public ResponseVo<CashTransferInOutVo> onAuthenticationError(AuthenticationException ex) {
        return null;
    }

    @Override
    public ResponseVo<CashTransferInOutVo> onInsufficientBalance(InsufficientBalanceException ex) {
        return null;
    }

    @Override
    public ResponseVo<CashTransferInOutVo> onInternalError(Throwable ex) {
        return null;
    }
}
