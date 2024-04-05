package com.nextgen.gameaggregator.vendor.yesbingo.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yesbingo.api.balance.BalanceAction;
import com.nextgen.gameaggregator.vendor.yesbingo.api.bet.BetAction;
import com.nextgen.gameaggregator.vendor.yesbingo.api.result.GameDetailResultAction;
import com.nextgen.gameaggregator.vendor.yesbingo.api.result.GameResultAction;
import com.nextgen.gameaggregator.vendor.yesbingo.api.rollback.CancelBetAction;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Actions;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.dto.VendorRequestDto;
import com.nextgen.gameaggregator.vendor.yesbingo.service.VendorService;
import com.nextgen.gameaggregator.vendor.yesbingo.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private BalanceAction balanceAction;
    @Autowired
    private BetAction betAction;
    @Autowired
    private GameResultAction gameResultAction;
    @Autowired
    private GameDetailResultAction gameDetailResultAction;
    @Autowired
    private CancelBetAction cancelBetAction;

    @PostMapping(path = EndPoints.ACTION + "/{id}")
    public ResponseVo balance(HttpServletRequest request, @PathVariable String id) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {

            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            VendorRequestDto vendorRequestDto = HttpService.convertQueryStringToDto(body, VendorRequestDto.class);

            // Validate request parameters (Non-database related)
            ValidationUtils.validateRequest(vendorRequestDto);

            // get decrypt data
            String decryptedData = this.getDecryptedData(vendorRequestDto.getX(), id);

            // Update the requestBody for easier debugging purposes
            httpRequestLog.setRequestBody(body + "&json=" + decryptedData);

            // Map the decrypted data
            ActionDto dto = HttpService.convertJsonToDto(decryptedData, ActionDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Do the appropriate action
            responseVo = this.doAction(dto, decryptedData, traceId, httpRequestLog);

        } catch (InvalidFormatException |
                 InvalidRequestException parameterInputErrorException) {
            responseVo.setStatus(ResponseCodes.PARAMETER_INPUT_ERROR);
            httpService.logError(httpRequestLog, parameterInputErrorException);

        } catch (InvalidAlgorithmParameterException |
                 NoSuchPaddingException |
                 IllegalBlockSizeException |
                 NoSuchAlgorithmException |
                 BadPaddingException |
                 InvalidKeyException |
                 CredentialNotFoundException |
                 JsonProcessingException exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);

        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);

        }

        return responseVo;

    }

    private void doValidation(ActionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

    }

    private String getDecryptedData(String encryptedData, String id)
            throws
            CredentialNotFoundException,
            InvalidAlgorithmParameterException,
            NoSuchPaddingException,
            IllegalBlockSizeException,
            NoSuchAlgorithmException,
            BadPaddingException,
            InvalidKeyException {

        // Get the first vendor line id from list
        Integer vendorLineId = vendorLineService.getVendorLineIdListByNameAndValue(Credentials.YESBINGO_ID, id);

        // Get the key and iv value with vendorLineId
        String key = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AES_KEY);
        String iv = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AES_IV);

        // Decrypt data
        return VendorService.decrypt(encryptedData, key, iv);

    }

    private ResponseVo doAction(ActionDto dto, String decryptedData, String traceId, HttpRequestLog httpRequestLog) {
        ResponseVo responseVo = new ResponseVo();

        switch (dto.getAction()) {
            case Actions.BALANCE -> {
                log.info("Yesbingo BALANCE: " + decryptedData);
                balanceAction.balance(httpRequestLog, traceId, decryptedData, responseVo);
            }
            case Actions.BET -> {
                // For Bingo & Slot = Bet
                betAction.bet(httpRequestLog, traceId, decryptedData, responseVo);
            }
            case Actions.GAME_RESULT -> {
                // For Bingo & Slot = Win
                gameResultAction.gameResult(httpRequestLog, traceId, decryptedData, responseVo);
            }
            case Actions.GAME_DETAIL_RESULT -> {
                // For fish game is Bet + Win
                gameDetailResultAction.gameDetailResult(httpRequestLog, traceId, decryptedData, responseVo);
            }
            case Actions.CANCEL_BET -> {
                cancelBetAction.cancelBet(httpRequestLog, traceId, decryptedData, responseVo);
            }
            // If the header does not match any of the expected values, return an error response
            default -> responseVo.setStatus(ResponseCodes.UNKNOWN_ACTION);
        }

        return responseVo;

    }

}
