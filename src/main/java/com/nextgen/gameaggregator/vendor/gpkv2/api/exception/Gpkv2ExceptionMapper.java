package com.nextgen.gameaggregator.vendor.gpkv2.api.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkv2.vo.CommonVo;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class Gpkv2ExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCodes.INSUFFICIENT_BALANCE);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        // BetController & BetResultController already has returnSuccessOnDuplicate set to true
        // In normal case, this exception should never be thrown, unless there is a bug
        // therefore we map to internal server exception
        return onInternalError(new InternalServerException(ex.getMessage(), ex));
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        CommonVo commonVo=new CommonVo();
        commonVo.setCode(ResponseCodes.SUCCESS.getCode());
        commonVo.setPlayer_id(ex.getContext().getVendorPlayerUsername());
        commonVo.setBalance("0");
        commonVo.setTimestamp(System.currentTimeMillis());
        return new VendorErrorResponse(commonVo);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }
    private VendorErrorResponse getErrorResponse(ResponseCodes responseCodes) {
        CommonVo vo = new CommonVo();
        vo.setErrorResponse(responseCodes);
        return new VendorErrorResponse(vo);
    }
}