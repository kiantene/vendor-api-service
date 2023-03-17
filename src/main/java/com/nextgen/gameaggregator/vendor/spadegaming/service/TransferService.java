package com.nextgen.gameaggregator.vendor.spadegaming.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.dto.TransferDto;
import com.nextgen.gameaggregator.vendor.spadegaming.vo.TransferVo;

@Service
public class TransferService {
    @Autowired
    private HttpService httpService;

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private WalletService walletService;
    
    public TransferVo transfer(HttpServletRequest request) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);
        
        // Get the request body and trace ID from the logging
        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getTraceId();

        // Create new TransferVo objects
        TransferVo transferVo = new TransferVo();
        transferVo.setMerchantCode(Credentials.MERCHANT_CODE);
        transferVo.setSerialNo(traceId);

        try {
            // Convert the request body to an TransferDto object
            TransferDto dto = HttpService.convertJsonToDto(body, TransferDto.class);
            // Verify the user token and get the corresponding game session
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAcctId());

            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);

            transferVo.setTransferId(dto.getTransferId());
            transferVo.setMerchantCode(Credentials.MERCHANT_CODE);
            transferVo.setMerchantTxId("20130813014319279367");
            transferVo.setAcctId(gameSession.getVendorPlayerUsername());
            transferVo.setBalance(betEvent.getLastBalance());
            transferVo.setMsg(ResponseCode.SUCCESS.description);
            transferVo.setResponseCode(ResponseCode.SUCCESS);
            transferVo.setSerialNo(traceId);
            
        } catch (JsonProcessingException jsonProcessingException) {
            transferVo.setResponseCode(ResponseCode.INVALID_FORMAT);

        } catch (AuthenticationException authenticationException) {
            transferVo.setResponseCode(ResponseCode.TOKEN_VALIDATION_FAILED);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (InsufficientBalanceException insufficientBalanceException){

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {

        } finally {
            // End the HTTP request logging and return the AuthBalanceVo object
            httpService.end(httpRequestLog, transferVo);
             
        }

        return transferVo;
    }
}
