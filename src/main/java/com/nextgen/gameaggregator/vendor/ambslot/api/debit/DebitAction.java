package com.nextgen.gameaggregator.vendor.ambslot.api.debit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.ga.writer.RawSettledBetRepository;
import com.nextgen.gameaggregator.repository.ga.writer.RawUnsettledBetRepository;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ambslot.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ambslot.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ambslot.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ambslot.service.VendorService;
import com.nextgen.gameaggregator.vendor.ambslot.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.DataVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.StatusVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.WalletVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class DebitAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    VendorService vendorService;
    @Autowired
    private WalletAdjustmentService walletAdjustmentService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;
    @Autowired
    private RawSettledBetRepository rawSettledBetRepository;

    @PostMapping(path = EndPoints.BET)
    public DebitVo debit(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        DebitVo debitVo = new DebitVo();
        StatusVo statusVo = new StatusVo();
        BalanceVo balanceVo = new BalanceVo();
        DataVo dataVo = new DataVo();
        WalletVo walletVo = new WalletVo();

        UnsettledBet unsettledBet = null;

        Double before_bet_balance = null;

        BigDecimal balance = null;

        DebitDto debitDto = new DebitDto();
        GameSession gameSession = new GameSession();

        String dateTime = vendorService.convertUnixToDateTime(System.currentTimeMillis());

        try{
            String body = httpRequestLog.getRequestBody();

            // get x-ambslot-signature value for validation
            Map<String,String> header = vendorService.headersToHashMap(request);

            debitDto = httpService.convertJsonToDto(body, DebitDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(debitDto);

            // Verify session token
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(debitDto.getUsername(), debitDto.getGameId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(debitDto, gameSession, header.get("x-ambslot-signature"), body);

            // Set it as unsettle status even the bet request will show is end round
            BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, httpRequestLog.getRequestBody(), httpRequestLog);
            balance = betEvent.getLastBalance();

            // Retrieve unsettle data
            unsettledBet = rawUnsettledBetRepository.findByVendorPlayerIdAndExternalTransactionId(gameSession.getVendorPlayerId(), betEvent.getBetInformation().getExternalTransactionId());

            // Get wallet balance before bet
            before_bet_balance = unsettledBet.getBetAmount().setScale(2, RoundingMode.DOWN).doubleValue() + unsettledBet.getBalance().setScale(2, RoundingMode.DOWN).doubleValue();

            statusVo.setCode(ResponseCodes.SUCCESS);
            statusVo.setMessage(ResponseCodes.SUCCESS_MSG);

            debitVo.setStatus(statusVo);

            walletVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            walletVo.setLastUpdate(dateTime);

            balanceVo.setBefore(before_bet_balance);
            balanceVo.setAfter(balance.setScale(2, RoundingMode.DOWN).doubleValue());

            dataVo.setUsername(debitDto.getUsername());
            dataVo.setWallet(walletVo);
            dataVo.setBalance(balanceVo);
            dataVo.setRefId(debitDto.getTransactionId());

            debitVo.setData(dataVo);
        }catch(TransactionStillProcessingException |
                BetResultIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.DUPLICATED_TRANSACTION_ERROR);
            statusVo.setMessage(ResponseCodes.DUPLICATED_TRANSACTION_ERROR_MSG);

            debitVo.setStatus(statusVo);
        }catch(InsufficientBalanceException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INSUFFICIENT_BALANCE);
            statusVo.setMessage(ResponseCodes.INSUFFICIENT_BALANCE_MSG);

            debitVo.setStatus(statusVo);
        }catch(InvalidRequestException |
               JsonProcessingException |
               CredentialNotFoundException |
               InvalidPlayerException |
               AuthenticationException |
               CurrencyNotSupportedException |
               InvalidSignatureException |
               GameNotSupportedException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_REQUEST);
            statusVo.setMessage(ResponseCodes.INVALID_REQUEST_MSG);

            debitVo.setStatus(statusVo);
        }catch(InvalidAgentApiCredentialException |
               VendorCurrencyNotSupportException |
               DisabledAgentPlayerException |
               DisabledGameException |
               InvalidOperatorResponseException |
               DisabledVendorLineException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_ERROR_MSG);

            debitVo.setStatus(statusVo);
        }catch(InvalidCredentialsException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_AGENT);
            statusVo.setMessage(ResponseCodes.INVALID_AGENT_MSG);
        }catch(Exception e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_ERROR_MSG);

            debitVo.setStatus(statusVo);
        }finally{
            httpService.end(httpRequestLog, debitVo);
        }

        return debitVo;
    }

    private void doValidation(DebitDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(DebitDto dto, GameSession gameSession, String header, String body) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, GameNotSupportedException, CurrencyNotSupportedException, CredentialNotFoundException, InvalidRequestException, InvalidSignatureException, JsonProcessingException, InvalidCredentialsException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession,dto.getUsername());

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), InvalidPlayerException::new);

        // Verify vendor gameCode
        String game_code = vendorService.trimGameCode(gameSession.getVendorGameCode());
        ValidationUtils.isEquals(game_code, dto.getGameId(), GameNotSupportedException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify agent is same with credential
        String agent = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.prefix);
        ValidationUtils.isEquals(agent.toLowerCase(), dto.getAgent(), InvalidCredentialsException::new);

        // Verify header value
        String secret = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.secret);
        int iterations = 1000;

        // Convert JsonNode back to JSON string
        String convertedJsonString = vendorService.convertObjectMapper(body);

        String encrypted_value = vendorService.encryption(convertedJsonString, secret, iterations);

        if(!header.equals(encrypted_value)){
            throw new InvalidSignatureException();
        }
    }
}
