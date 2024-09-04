package com.nextgen.gameaggregator.vendor.cg.api.record;

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
    public RecordVo record(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        RecordVo responseVo = new RecordVo();
        RecordDto dto = new RecordDto();
        GameSession gameSession = new GameSession();

        try {
            dto = HttpService.convertQueryStringToDtoUrlDecode(httpRequestLog, RecordDto.class);

            this.doValidation(dto);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAccountId());

            this.doVerification(dto, gameSession);

            // initialize fixed value
            responseVo.getData().setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setChannelId(dto.getChannelId());

            responseVo = checkBetAvailable(gameSession, dto);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setErrorCode(ResponseCodes.SEAMLESS_INPUT_ERROR);
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (InvalidVendorLineException invalidVendorLineException) {
            responseVo.setErrorCode(ResponseCodes.CHANNEL_ID_ERROR);
            responseVo.setReturnTime(VendorService.returnTime());
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (AuthenticationException authenticationException) {
            responseVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_PLAYER);
            responseVo.setReturnTime(VendorService.returnTime());
            httpService.logError(httpRequestLog, authenticationException);
        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setErrorCode(ResponseCodes.SEAMLESS_UNKNOWN_TRANSACTION);
            responseVo.setReturnTime(VendorService.returnTime());
            httpService.logError(httpRequestLog, betNotFoundException);
        } catch (Exception exception) {
            responseVo.setErrorCode(ResponseCodes.UNKNOWN_ERROR);
            responseVo.setReturnTime(VendorService.returnTime());
            httpService.logError(httpRequestLog, exception);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(RecordDto dto) throws InvalidRequestException {
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
