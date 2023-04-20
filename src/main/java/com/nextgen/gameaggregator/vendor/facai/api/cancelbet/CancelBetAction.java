package com.nextgen.gameaggregator.vendor.facai.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
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

import jakarta.servlet.http.HttpServletRequest;

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

    @PostMapping(path = EndPoints.CANCEL_BET)
    public CommonVo cancelbet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo commonVo = new CommonVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            //Get vendor line id by agent code from vendor line credential
            Integer vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.AGENT_CODE, commonDto.getAgentCode());

            //Decrypt raw respond with key from vendor line credential
            String jsonParam = vendorService.aesDecrypt(commonDto.getParams(), vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AGENT_KEY));

            //map decrypted data(string json) into cancelBetDto
            CancelBetDto cancelbetDto = HttpService.convertJsonToDto(jsonParam, CancelBetDto.class);

            //Validate request parameters from vendor after decrypt (Non-database related)
            this.doDecryptValidation(cancelbetDto);

            //Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(cancelbetDto.getMemberAccount());
            BetHistory betHistory = betHistoryService.getBetTransactionByVendorTransactionId(Long.toString(cancelbetDto.getBankID()), vendorPlayer.getVendorId());
            RawGameSession rawGameSession = gameSessionService.verifyToken(betHistory.getGameSessionToken());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, cancelbetDto, rawGameSession, jsonParam);

            //revert the cancel bet if found transaction id
            commonVo.setErrorResponseCode(ResponseCodes.REVERT_CANCEL_BET);

        } catch (
                InvalidDecryptionException |
                InvalidEncryptionException |
                InvalidPlayerException |
                InvalidRequestException |
                BetNotFoundException |
                CurrencyNotSupportedException |
                JsonProcessingException |
                CredentialNotFoundException |
                DisabledGameException notExistException
        ) {
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
        ValidationUtils.validateRequest(dto);
    }

    private void doDecryptValidation(CancelBetDto dto) throws InvalidRequestException, InvalidPlayerException, CurrencyNotSupportedException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CommonDto commonDto, CancelBetDto cancelbetDto, RawGameSession rawGameSession, String jsonParam) throws InvalidRequestException, CurrencyNotSupportedException, InvalidPlayerException, CredentialNotFoundException, DisabledGameException, InvalidEncryptionException {

        //Verify received username is the same from game session
        ValidationUtils.isEquals(rawGameSession.getVendorPlayerUsername(), cancelbetDto.getMemberAccount(), InvalidPlayerException::new);

        //Verify received game id is the same from game session
        //comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(rawGameSession.getVendorGameCode(), Integer.toString(cancelbetDto.getGameID()), DisabledGameException::new);

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(rawGameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(rawGameSession.getVendorCurrencyCode(), cancelbetDto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify received Sign is the same from param value
        //MD5 encrypt
        String md5Param = vendorService.md5(jsonParam);
        ValidationUtils.isEquals(md5Param, commonDto.getSign(), InvalidRequestException::new);

        //Verify received agent code is the same from credential
        String AgentCode = vendorLineService.getCredentialValueByName(rawGameSession.getVendorLineId(), Credentials.AGENT_CODE);
        ValidationUtils.isEquals(AgentCode, commonDto.getAgentCode(), InvalidRequestException::new);

    }
}
