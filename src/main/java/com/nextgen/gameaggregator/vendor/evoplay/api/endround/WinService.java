package com.nextgen.gameaggregator.vendor.evoplay.api.endround;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
public class WinService {

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

    public ResponseVo win(CallbackDto callbackDto, GameSession gameSession, HttpRequestLog httpRequestLog, String traceId, String key)
            throws
            CurrencyNotSupportedException,
            InvalidRequestException,
            InvalidPlayerException,
            InvalidVendorLineException,
            AuthenticationException,
            DisabledAgentPlayerException,
            GameNotSupportedException,
            DisabledGameException,
            DisabledVendorLineException,
            CredentialNotFoundException,
            InvalidAgentApiCredentialException,
            BetResultIdempotentViolationException,
            MergedBetDataIntegrityException,
            InsufficientBalanceException,
            BetNotFoundException,
            InvalidOperatorResponseException,
            SettledBetIdempotentViolationException,
            TransactionStillProcessingException {

        callbackDto.getData().setDetailsDto(new Gson().fromJson(callbackDto.getData().getDetails(), DetailsDto.class));
        WinDto winDto = new ModelMapper().map(callbackDto, WinDto.class);

        this.doValidation(callbackDto);
        this.doVerification(callbackDto, gameSession, key);

        ResultType resultType = vendorService.calculateResultType(winDto.getBetAmount(), winDto.getWinAmount(), winDto.getJackpotAmount(), false);
        BigDecimal balance = walletService.processBetResult(traceId, gameSession, winDto, resultType, vendorService, httpRequestLog);

        ResponseDataVo responseDataVo = new ResponseDataVo();
        responseDataVo.setBalance(balance);
        responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());

        ResponseVo responseVo = new ResponseVo();
        responseVo.setData(responseDataVo);

        return responseVo;
    }

    private void doValidation(CallbackDto dto) throws InvalidRequestException, CurrencyNotSupportedException {

        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getData());
    }

    private void doVerification(CallbackDto dto, GameSession gameSession, String key)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            AuthenticationException,
            CurrencyNotSupportedException,
            GameNotSupportedException {

        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        String signature = VendorService.generateSignature(dto, key);
        ValidationUtils.isEquals(signature, dto.getSignature(), AuthenticationException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getData().getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getData().getDetailsDto().getGame().getGame_id(), GameNotSupportedException::new);
    }
}
