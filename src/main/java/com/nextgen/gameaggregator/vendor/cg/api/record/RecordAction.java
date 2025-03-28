package com.nextgen.gameaggregator.vendor.cg.api.record;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cg.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EndPoints.PATH)
public class RecordAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;

    @Autowired
    public RecordAction(HttpService httpService,
                        GameSessionService gameSessionService,
                        VendorService vendorService,
                        VendorLineService vendorLineService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(EndPoints.RECORD)
    public String record(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        RecordVo recordVo = new RecordVo();
        CommonDto dto = new CommonDto();

        try {
            //convert body into dto
            dto = HttpService.convertQueryStringToDto(httpRequestLog, CommonDto.class);
            dto.setData(VendorService.urlDecode(dto.getData()));

            this.doValidation(dto);

            String decryptedData = vendorService.decryptData(dto.getData(), dto.getChannelId());//we get the json here
            httpRequestLog.setRequestBody(decryptedData);
            RecordDto recordDto = HttpService.convertJsonToDto(decryptedData, RecordDto.class);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(recordDto.getAccountId());

            this.doVerification(recordDto, gameSession);

            // initialize fixed value
            recordVo.getData().setCurrency(gameSession.getVendorCurrencyCode());
            recordVo.setChannelId(dto.getChannelId());

            recordVo = checkBetAvailable(gameSession, recordDto);
        } catch (InvalidRequestException invalidRequestException) {
            recordVo.setErrorCode(ResponseCodes.SEAMLESS_INPUT_ERROR);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (InvalidVendorLineException invalidVendorLineException) {
            recordVo.setErrorCode(ResponseCodes.CHANNEL_ID_ERROR);
            recordVo.setReturnTime(VendorService.returnTime());
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (AuthenticationException authenticationException) {
            recordVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_PLAYER);
            recordVo.setReturnTime(VendorService.returnTime());
            httpService.logError(httpRequestLog, authenticationException);
        } catch (BetNotFoundException betNotFoundException) {
            recordVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_TRANSACTION);
            recordVo.setReturnTime(VendorService.returnTime());
            httpService.logError(httpRequestLog, betNotFoundException);
        } catch (Exception exception) {
            recordVo.setErrorCode(ResponseCodes.UNKNOWN_ERROR);
            recordVo.setReturnTime(VendorService.returnTime());
            httpService.logError(httpRequestLog, exception);
        } finally {
            try {
                String jsonString = new Gson().toJson(recordVo);
                recordVo.setEncrypt(vendorService.encryptResponse(jsonString, dto.getChannelId())); //encrypt the whole vo include error
                httpService.end(httpRequestLog, recordVo);
            } catch (CredentialNotFoundException e) {
                httpService.logError(httpRequestLog, e);
            }
        }
        return recordVo.getEncrypt();
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RecordDto dto, GameSession gameSession) throws InvalidVendorLineException, CredentialNotFoundException {
        String channelId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CHANNEL_ID);
        ValidationUtils.isEquals(channelId, dto.getChannelId(), InvalidVendorLineException::new);

    }

    private RecordVo checkBetAvailable(GameSession gameSession, RecordDto dto) throws BetNotFoundException {

        RecordVo recordVo;

        recordVo = vendorService.settledBetIdempotentCheck(gameSession, dto);
        if (recordVo == null) {
            recordVo = vendorService.unsettledBetIdempotentCheck(gameSession, dto);
        }

        if (recordVo == null) { //if after both queries still null
            throw new BetNotFoundException();
        }

        return recordVo;
    }

}
