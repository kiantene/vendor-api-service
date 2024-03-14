package com.nextgen.gameaggregator.vendor.ambslot.api.credit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
public class CreditAction {
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
    private RawSettledBetRepository rawSettledBetRepository;
    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;
    @Autowired
    private UnsettledBetService unsettledBetService;

    @PostMapping(path = EndPoints.PAYOUT)
    public CreditVo payout(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CreditVo creditVo = new CreditVo();
        StatusVo statusVo = new StatusVo();
        BalanceVo balanceVo = new BalanceVo();
        DataVo dataVo = new DataVo();
        WalletVo walletVo = new WalletVo();

        GameSession gameSession = new GameSession();

        CreditDto creditDto = new CreditDto();

        BigDecimal balance = null;

        SettledBet settledBet = null;
        UnsettledBet unsettledBet = null;

        Double before_bet_balance = null;

        String dateTime = vendorService.convertUnixToDateTime(System.currentTimeMillis());

        try{
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // get x-ambslot-signature value for validation
            Map<String,String> header = vendorService.headersToHashMap(request);

            creditDto = httpService.convertJsonToDto(body, CreditDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(creditDto);

            // Verify session token
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(creditDto.getUsername(), creditDto.getGameId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto,gameSession, header.get("x-ambslot-signature"), body);

            ResultType resultType = vendorService.generateResultType(creditDto.getAmount(), creditDto.getIsEndRound());

            balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);

            if(creditDto.getIsEndRound().equals(true)){
                // Retrieve settled data
                settledBet = rawSettledBetRepository.findByVendorPlayerIdAndExternalTransactionId(gameSession.getVendorPlayerId(), creditDto.getTransactionId());

                before_bet_balance = settledBet.getBetAmount().setScale(2, RoundingMode.DOWN).doubleValue() + settledBet.getBalance().setScale(2, RoundingMode.DOWN).doubleValue();
            }else{
                // Retrieve unsettle data
                unsettledBet = unsettledBetService.getByVendorIdAndExternalTransactionId(gameSession.getVendorId(), creditDto.getTransactionId());

                before_bet_balance = unsettledBet.getBetAmount().setScale(2, RoundingMode.DOWN).doubleValue() + unsettledBet.getBalance().setScale(2, RoundingMode.DOWN).doubleValue();
            }

            statusVo.setCode(ResponseCodes.SUCCESS);
            statusVo.setMessage(ResponseCodes.SUCCESS_MSG);

            creditVo.setStatus(statusVo);

            walletVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            walletVo.setLastUpdate(dateTime);

            balanceVo.setBefore(before_bet_balance);
            balanceVo.setAfter(balance.setScale(2, RoundingMode.DOWN).doubleValue());

            dataVo.setUsername(creditDto.getUsername());
            dataVo.setWallet(walletVo);
            dataVo.setBalance(balanceVo);
            dataVo.setRefId(creditDto.getTransactionId());

            creditVo.setData(dataVo);
        }catch(TransactionStillProcessingException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.DUPLICATED_TRANSACTION_ERROR);
            statusVo.setMessage(ResponseCodes.DUPLICATED_TRANSACTION_ERROR_MSG);

            creditVo.setStatus(statusVo);
        }catch(BetResultIdempotentViolationException e){
            // avoid credit occur error and causing retry whole transaction request from bet to settle request
            if(creditDto.getIsEndRound().equals(true)){
                // Retrieve settled data
                settledBet = rawSettledBetRepository.findByVendorPlayerIdAndExternalTransactionId(gameSession.getVendorPlayerId(), creditDto.getTransactionId());

                before_bet_balance = settledBet.getBetAmount().setScale(2, RoundingMode.DOWN).doubleValue() + settledBet.getBalance().setScale(2, RoundingMode.DOWN).doubleValue();

                walletVo.setBalance(settledBet.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
                balanceVo.setAfter(settledBet.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
            }else{
                // Retrieve unsettle data
                unsettledBet = rawUnsettledBetRepository.findByVendorPlayerIdAndExternalTransactionId(gameSession.getVendorPlayerId(), creditDto.getTransactionId());

                before_bet_balance = unsettledBet.getBetAmount().setScale(2, RoundingMode.DOWN).doubleValue() + unsettledBet.getBalance().setScale(2, RoundingMode.DOWN).doubleValue();

                walletVo.setBalance(unsettledBet.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
                balanceVo.setAfter(unsettledBet.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
            }

            statusVo.setCode(ResponseCodes.SUCCESS);
            statusVo.setMessage(ResponseCodes.SUCCESS_MSG);

            creditVo.setStatus(statusVo);

            walletVo.setLastUpdate(dateTime);

            balanceVo.setBefore(before_bet_balance);

            dataVo.setUsername(creditDto.getUsername());
            dataVo.setWallet(walletVo);
            dataVo.setBalance(balanceVo);
            dataVo.setRefId(creditDto.getTransactionId());

            creditVo.setData(dataVo);

        }catch(InvalidRequestException |
               JsonProcessingException |
               GameNotSupportedException |
               CurrencyNotSupportedException |
               AuthenticationException |
               InvalidPlayerException |
               BetNotFoundException |
               InvalidSignatureException |
               CredentialNotFoundException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_REQUEST);
            statusVo.setMessage(ResponseCodes.INVALID_REQUEST_MSG);

            creditVo.setStatus(statusVo);
        }catch(VendorCurrencyNotSupportException |
               InsufficientBalanceException |
               InvalidOperatorResponseException |
               DisabledVendorLineException |
               InvalidAgentApiCredentialException |
               DisabledAgentPlayerException |
               MergedBetDataIntegrityException |
               DisabledGameException e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_ERROR_MSG);

            creditVo.setStatus(statusVo);
        }catch(Exception e){
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_ERROR_MSG);

            creditVo.setStatus(statusVo);
        }finally{
            httpService.end(httpRequestLog, creditVo);
        }

        return creditVo;
    }

    private void doValidation(CreditDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CreditDto dto, GameSession gameSession, String header, String body) throws DisabledGameException, DisabledAgentPlayerException, DisabledVendorLineException, InvalidPlayerException, GameNotSupportedException, CurrencyNotSupportedException, CredentialNotFoundException, InvalidRequestException, InvalidSignatureException, JsonProcessingException {
        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), InvalidPlayerException::new);

        // Verify vendor gameCode
        String game_code = vendorService.trimGameCode(gameSession.getVendorGameCode());
        ValidationUtils.isEquals(game_code, dto.getGameId(), GameNotSupportedException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify agent is same with credential
        String agent = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.prefix);
        ValidationUtils.isEquals(agent.toLowerCase(), dto.getAgent(), InvalidRequestException::new);

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
