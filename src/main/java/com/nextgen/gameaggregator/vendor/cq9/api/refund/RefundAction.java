package com.nextgen.gameaggregator.vendor.cq9.api.refund;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RefundAction {
    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private UnsettledBetService unsettledBetService;

    @PostMapping(path = EndPoints.REFUND, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseVo<CommonVo> refund(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String wToken = request.getHeader("wtoken");

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        CommonVo commonVo = new CommonVo();
        String vendorCurrencyCode = null;

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            RefundDto refundDto = HttpService.convertQueryStringToDtoUrlDecode(body, RefundDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(refundDto, wToken);

            // 2. Gather require data
            // TODO: get vendor id by vendor code
            Integer vendorId = vendorService.findVendorByCode(Credentials.VENDOR_CODE).getId();
            UnsettledBet unsettledBet = unsettledBetService.getByVendorIdAndExternalTransactionId(vendorId, refundDto.getMtcode());

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(unsettledBet.getGameSessionToken());
            vendorCurrencyCode = gameSession.getVendorCurrencyCode();

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(refundDto, wToken, unsettledBet);

            // 5. Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, refundDto, gameSession, vendorService);

            commonVo.setBalance(balance);
            commonVo.setCurrency(vendorCurrencyCode);
            responseVo.setData(commonVo);

        } catch (BetNotFoundException betNotFoundException) {
            statusVo.setCode(ResponseCodes.TRANSACTION_RECORD_NOT_FOUND);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            //if found the bet in settled status
            if (betResultIdempotentViolationException.getStatus() == BetStatus.SETTLED.code) {
                statusVo.setCode(ResponseCodes.SERVER_ERROR);

            } else {
                //if found the bet other in settled status (cancel / refund)
                commonVo.setBalance(betResultIdempotentViolationException.getBalance());
                commonVo.setCurrency(vendorCurrencyCode);
                responseVo.setData(commonVo);

            }

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            statusVo.setCode(ResponseCodes.SERVER_ERROR);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus() == 15) {
                //Operator Bet not found
                statusVo.setCode(ResponseCodes.TRANSACTION_RECORD_NOT_FOUND);
            } else {
                //Other operator errors
                statusVo.setCode(ResponseCodes.SERVER_ERROR);
            }

            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (AuthenticationException |
                 CredentialNotFoundException |
                 InvalidAgentApiCredentialException |
                 InvalidVendorLineException playerNotFoundException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);


        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(RefundDto refundDto, String wToken) throws InvalidRequestException {
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(refundDto);
    }

    private void doVerification(RefundDto refundDto, String wToken, UnsettledBet unsettledBet) throws InvalidVendorLineException, CredentialNotFoundException {
        // 3. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(unsettledBet.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 4. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);
    }
}
