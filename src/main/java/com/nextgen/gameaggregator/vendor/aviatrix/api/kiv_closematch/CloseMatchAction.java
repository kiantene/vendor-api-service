package com.nextgen.gameaggregator.vendor.aviatrix.api.kiv_closematch;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aviatrix.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@RequestMapping(EndPoints.PATH)
public class CloseMatchAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;

    @Autowired
    public CloseMatchAction(HttpService httpService,
                            GameSessionService gameSessionService,
                            VendorLineService vendorLineService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
    }

    //Endpoint no longer in use

    //@PostMapping(EndPoints.CLOSE_MATCH)
    public ResponseEntity<ResponseVo> closeMatch(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        CloseMatchDto dto;
        HttpStatus httpStatus = HttpStatus.OK;

        try {
            dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), CloseMatchDto.class);

            this.doValidation(dto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());

            this.doVerification(gameSession, dto);

        } catch (AuthenticationException authenticationException) {
            responseVo.setMessage(ResponseCodes.PLAYER_NOT_FOUND);
            httpStatus = HttpStatus.NOT_FOUND;
            httpService.logError(httpRequestLog, authenticationException);
        } catch (GameNotSupportedException gameNotSupportedException) {
            responseVo.setMessage(ResponseCodes.PRODUCT_NOT_FOUND);
            httpStatus = HttpStatus.NOT_FOUND;
            httpService.logError(httpRequestLog, gameNotSupportedException);
        } catch (InvalidVendorLineException | CredentialNotFoundException invalidVendorLineException) {
            responseVo.setMessage(ResponseCodes.PLATFORM_NOT_FOUND);
            httpStatus = HttpStatus.NOT_FOUND;
            httpService.logError(httpRequestLog, invalidVendorLineException);
        } catch (InvalidFormatException |
                 NullPointerException |
                 InvalidRequestException invalidRequestException) {
            responseVo.setMessage(ResponseCodes.INVALID_REQUEST);
            httpStatus = HttpStatus.BAD_REQUEST;
            httpService.logError(httpRequestLog, invalidRequestException);
        } catch (Exception e) {
            responseVo.setMessage(ResponseCodes.UNKNOWN_ERROR);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return new ResponseEntity<>(responseVo, httpStatus);
    }

    private void doValidation(CloseMatchDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession, CloseMatchDto dto) throws CredentialNotFoundException, InvalidVendorLineException, GameNotSupportedException {
        String channelId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CID);
        ValidationUtils.isEquals(channelId, dto.getCid(), InvalidVendorLineException::new);

        //check player id is same as session id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getProductId(), GameNotSupportedException::new);
    }
}
