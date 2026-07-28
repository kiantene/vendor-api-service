package com.nextgen.gameaggregator.vendor.evoplay.api.v2.exception;


import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Refund;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class EvoPlayExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCodes.ERROR);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.ERROR);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        ResponseVo responseVo = ResponseVo.builder()
                .status(ResponseCodes.INSUFFICIENT_BALANCE_ERROR.status)
                .error(mapResponseData())
                .build();
        return new VendorErrorResponse(HttpStatus.OK, responseVo);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.ERROR);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.INVALID_REQUEST_ERROR);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        ResponseVo responseVo = ResponseVo.builder()
                .status(ResponseCodes.SUCCESS.status)
                .data(mapResponseData(ex))
                .build();
        return new VendorErrorResponse(HttpStatus.OK, responseVo);
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return getErrorResponse(ResponseCodes.IDEMPOTENT_ERROR);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCodes.INVALID_REQUEST_ERROR);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCodes.INVALID_REQUEST_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(RuntimeException.class) || ex.isCausedBy(InvalidOperatorResponseException.class)) {
            ResponseDataVo responseDataVo = ResponseDataVo.builder()
                    .scope(Scope.INTERNAL)
                    .no_refund(Refund.ONE)
                    .message(ResponseCodes.PROCESSING_ERROR.message)
                    .build();
            ResponseVo responseVo = ResponseVo.builder()
                    .status(ResponseCodes.PROCESSING_ERROR.status)
                    .error(responseDataVo)
                    .build();
            return new VendorErrorResponse(HttpStatus.OK, responseVo);
        }
        if (ex.isCausedBy(RollbackNotAllowedException.class)) {
            return getErrorResponse(ResponseCodes.SUCCESS);
        }

        return getErrorResponse(ResponseCodes.ERROR);
    }

    private ResponseDataVo mapResponseData(DuplicateRequestException ex) {
        return ResponseDataVo.builder()
                .balance(ex.getTransaction().getBalance())
                .currency(ex.getTransaction().getCurrency())
                .build();
    }

    private ResponseDataVo mapResponseData() {
        return ResponseDataVo.builder()
                .scope(Scope.INTERNAL)
                .no_refund(Refund.ONE)
                .message(ResponseCodes.INSUFFICIENT_BALANCE_ERROR.message)
                .build();
    }

    private VendorErrorResponse getErrorResponse(ResponseCodes responseCodes) {
        ResponseVo response = new ResponseVo();
        response.setResponseCode(responseCodes);
        return new VendorErrorResponse(HttpStatus.OK, response);
    }
}
