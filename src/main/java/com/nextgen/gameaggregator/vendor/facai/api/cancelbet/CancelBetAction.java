package com.nextgen.gameaggregator.vendor.facai.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.facai.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import com.nextgen.gameaggregator.vendor.facai.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelBetAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.CANCEL_SLOT_BET)
    public CommonVo cancelbet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo commonVo = new CommonVo();
        //commonVo.setResult(0);
        //commonVo.setMainPoints(1000.00);
        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            //TODO pending PG update core function to get appKey
            //Decrypt raw respond
            String jsonParam = vendorService.aesDecrypt(commonDto.getParams(), "Q7RaR8CUbwZ0roD2");

            //map decrypted data(string json) into balanceDto
            CancelBetDto cancelbetDto = HttpService.convertJsonToDto(jsonParam, CancelBetDto.class);

            //Validate request parameters from vendor after decrypt (Non-database related)
            this.doDecryptValidation(cancelbetDto);

            //Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(cancelbetDto.getMemberAccount());
            BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionId(Long.toString(cancelbetDto.getBankID()), vendorPlayer.getVendorId());

            //revert the cancel bet if found transaction id
            commonVo.setErrorResponseCode(ResponseCodes.REVERT_CANCEL_BET);

        } catch (InvalidDecryptionException invalidDecryptionException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (InvalidPlayerException invalidPlayerException) {
            commonVo.setErrorResponseCode(ResponseCodes.TRANSACTION_NOT_EXIST);
        } catch (InvalidRequestException invalidRequestException) {
            commonVo.setErrorResponseCode(ResponseCodes.TRANSACTION_NOT_EXIST);
        } catch (BetNotFoundException betNotFoundException) {
            commonVo.setErrorResponseCode(ResponseCodes.TRANSACTION_NOT_EXIST);
        }catch (CurrencyNotSupportedException currencyNotSupportedException) {
            commonVo.setErrorResponseCode(ResponseCodes.TRANSACTION_NOT_EXIST);
        } catch (JsonProcessingException jsonProcessingException) {
            commonVo.setErrorResponseCode(ResponseCodes.TRANSACTION_NOT_EXIST);
        } catch (Exception exception) {
            commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;

    }

    private void doValidation(CommonDto dto) throws InvalidRequestException, CurrencyNotSupportedException {
        // General validation
        //ValidationUtils.validateRequest(dto);
        if(!vendorService.isValidString(dto.getAgentCode())) {throw new InvalidRequestException();}
        if(!vendorService.isValidString(dto.getSign())) {throw new InvalidRequestException();}
        if(!vendorService.isValidString(dto.getCurrency())) {throw new CurrencyNotSupportedException();}
        if(!vendorService.isValidStringLength(dto.getCurrency(), 3, 3)) {throw new CurrencyNotSupportedException();}
    }

    private void doDecryptValidation(CancelBetDto dto) throws InvalidRequestException, InvalidPlayerException, CurrencyNotSupportedException {
        // General validation
        //ValidationUtils.validateRequest(dto);
        if(!vendorService.isValidString(dto.getMemberAccount())) {throw new InvalidPlayerException();}
        if(!vendorService.isValidStringLength(dto.getMemberAccount(), 2, 30)) {throw new InvalidPlayerException();}
        if(dto.getBankID() == null) {throw new InvalidRequestException();}
        if(!vendorService.isValidString(dto.getCurrency())) {throw new CurrencyNotSupportedException();}
        if(!vendorService.isValidStringLength(dto.getCurrency(), 3, 3)) {throw new CurrencyNotSupportedException();}
        if(!vendorService.isValidInteger(dto.getGameID())) {throw new InvalidRequestException();}
        if(dto.getTs() == null || !vendorService.isValidTimestamp(dto.getTs())) {throw new InvalidRequestException();}
    }
}
