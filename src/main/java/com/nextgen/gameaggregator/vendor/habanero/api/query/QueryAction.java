package com.nextgen.gameaggregator.vendor.habanero.api.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.constant.Credentials;
import com.nextgen.gameaggregator.vendor.habanero.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class QueryAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.QUERY)
    public ResponseEntity<QueryVo> balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        QueryVo responseVo = new QueryVo();
        Integer httpStatus = HttpStatus.SC_OK;

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into authDto
            QueryDto queryDto = HttpService.convertJsonToDto(body, QueryDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(queryDto);

            //Get GameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(queryDto.getQueryRequestDto().getAccountId(), queryDto.getBaseGame().getKeyName());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(queryDto, gameSession);

            //Check bet record available from settle and unsettle table
            this.checkBetAvailable(gameSession, queryDto.getQueryRequestDto());

            // bet not found return false respond
            responseVo.setResponseCode(ResponseCodes.QUERY_FALSE);

        } catch (
                AuthenticationException |
                InvalidRequestException |
                NoAvailableLineException |
                JsonProcessingException |
                CredentialNotFoundException generalException
        ) {
            responseVo.setResponseCode(ResponseCodes.QUERY_FALSE);

        } catch (TransactionStillProcessingException TransactionStillProcessingException) {
            //return invalid respond to trigger vendor resend when record still in processing
            responseVo.setResponseCode(ResponseCodes.RETRY_ERROR);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            // bet found return true respond
            responseVo.setResponseCode(ResponseCodes.QUERY_SUCCESS);

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.QUERY_FALSE);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);

        }

        if (responseVo.getFundTransferResponseVo().getStatusVo().getRetryStatus() != null) {
            //return invalid respond 404 to trigger vendor resend when record still in processing
            responseVo = null;
            httpStatus = HttpStatus.SC_NOT_FOUND;
        }

        return new ResponseEntity<>(responseVo, HttpStatusCode.valueOf(httpStatus));
    }

    private void doValidation(QueryDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getBaseGame());
        ValidationUtils.validateRequest(dto.getSubAuth());
        ValidationUtils.validateRequest(dto.getQueryRequestDto());
        ValidationUtils.isEquals("queryrequest", dto.getType(), InvalidRequestException::new);
    }

    private void doVerification(QueryDto dto, GameSession gameSession) throws NoAvailableLineException, CredentialNotFoundException {

        //Verify received passkey is the same from credential
        String passkey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PASSKEY);
        ValidationUtils.isEquals(passkey, dto.getSubAuth().getPasskey(), NoAvailableLineException::new);

        //Verify received brand id is the same from credential
        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        ValidationUtils.isEquals(brandId, dto.getSubAuth().getBrandid(), NoAvailableLineException::new);

    }

    private void checkBetAvailable(GameSession gameSession, QueryRequestDto queryRequestDto) throws TransactionStillProcessingException, BetResultIdempotentViolationException {

        // settle bet Idempotent Check
        vendorService.settledBetIdempotentCheck(gameSession, queryRequestDto.getInitialDebitTransferId(), queryRequestDto.getGameInstanceId());

        // unsettle bet Idempotent Check
        vendorService.unsettledBetIdempotentCheck(gameSession, queryRequestDto.getTransferId(), queryRequestDto.getGameInstanceId());

    }

}
