package com.nextgen.gameaggregator.vendor.evoplay.api.refund;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DetailsDto;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
@Slf4j
public class RefundService {

    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;

    public ResponseVo refund(CallbackDto callbackDto, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog)
            throws
            CurrencyNotSupportedException,
            InvalidRequestException,
            InvalidPlayerException,
            InvalidVendorLineException,
            DisabledAgentPlayerException,
            GameNotSupportedException,
            DisabledGameException,
            DisabledVendorLineException,
            CredentialNotFoundException,
            InvalidAgentApiCredentialException,
            RecordNotFoundException,
            BetRefundIdempotentViolationException,
            InvalidOperatorResponseException,
            BetNotFoundException,
            CouchbaseDataIntegrityException,
            BetResultIdempotentViolationException,
            TransactionStillProcessingException,
            VendorCurrencyNotSupportException, InvalidFormatException {

        callbackDto.getData().setDetailsDto(new Gson().fromJson(callbackDto.getData().getDetails(), DetailsDto.class));
        RefundDto refundDto = new ModelMapper().map(callbackDto, RefundDto.class);

        // Validate request parameters (Non-database calls)
        this.doValidation(refundDto);
        this.doVerification(refundDto, gameSession);

        // Retrieve the latest wallet balance from Operator
        BigDecimal balance = walletService.processRollback(traceId, refundDto, gameSession, vendorService, httpRequestLog);

        // Set Vendor player username + Balance + Currency
        ResponseDataVo responseDataVo = new ResponseDataVo();
        responseDataVo.setBalance(balance);
        responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());

        // Set data for response vo
        ResponseVo responseVo = new ResponseVo();
        responseVo.setData(responseDataVo);

        return responseVo;
    }

    private void doValidation(RefundDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getData());
    }

    private void doVerification(RefundDto dto, GameSession gameSession)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            CurrencyNotSupportedException,
            GameNotSupportedException {

        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getData().getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getData().getDetailsDto().getGame().getGame_id(), GameNotSupportedException::new);
    }

}
