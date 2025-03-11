package com.nextgen.gameaggregator.vendor.dreamgaming.api.bet;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.TransferType;
import com.nextgen.gameaggregator.vendor.dreamgaming.dto.DetailDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.dreamgaming.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.CredentialException;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final WalletAdjustmentService walletAdjustmentService;
    private final SettledBetService settledBetService;

    public BetAction(WalletService walletService,
                     HttpService httpService,
                     ValidationService validationService,
                     VendorService vendorService, VendorLineService vendorLineService, WalletAdjustmentService walletAdjustmentService, SettledBetService settledBetService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.walletAdjustmentService = walletAdjustmentService;
        this.settledBetService = settledBetService;
    }

    @PostMapping(path = EndPoints.TRANSFER)
    public ResponseVo transaction(@PathVariable("agentName") String agentName,
                                  HttpServletRequest request) throws CredentialNotFoundException {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();
        ResponseVo vo = new ResponseVo();
        BetDto betDto = new BetDto();
        BigDecimal balance;
        GameSession gameSession;

        try {
            betDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), BetDto.class);

            betDto.setDetailDto(HttpService.convertJsonToDto(VendorService.removeLeadingZero(betDto.getDetail()), DetailDto.class));
            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // Verify session
            gameSession = vendorService.checkGameSession(traceId, betDto);

            // Verify parameters (Verify against database values)
            this.doVerification(betDto, gameSession);
            switch (betDto.getType()) {
                case TransferType.BET:
                    //Bet
                    balance = getCurrentBalance(traceId, gameSession, httpRequestLog);
                    walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);

                    vo.getMember().setAmount(betDto.getBetAmount().abs().negate());
                    vo.getMember().setBalance(balance);
                    break;

                case TransferType.PAYOUT:
                    //Settle
                    balance = getCurrentBalance(traceId, gameSession, httpRequestLog);
                    ResultType updatedResultType = vendorService.calculateResultType(betDto.getBetAmount(), betDto.getWinAmount(), betDto.getJackpotAmount(), false);
                    walletService.processBetResult(traceId, gameSession, betDto, updatedResultType, vendorService, httpRequestLog);

                    vo.getMember().setAmount(betDto.getWinAmount());
                    vo.getMember().setBalance(balance);
                    break;

                case TransferType.APPEND:
                    //APPEND
                    AppendDto appendDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), AppendDto.class);
                    appendDto.setDetailDto(HttpService.convertJsonToDto(VendorService.removeLeadingZero(appendDto.getDetail()), DetailDto.class));
                    // Validate request parameters from vendor (Non-database related)
                    this.doValidation(appendDto);

                    // Get settle bet to calculate adjustment amount
                    SettledBet settledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(appendDto.getParentBetId(), appendDto.getRoundId(), gameSession.getVendorId(), gameSession.getVendorPlayerId());
                    appendDto.setAdjustmentAmount(appendDto.getMember().getAmount().subtract(settledBet.getWinAmount()));
                    balance = getCurrentBalance(traceId, gameSession, httpRequestLog);
                    walletAdjustmentService.processAdjustment(traceId, gameSession, appendDto, httpRequestLog);

                    vo.getMember().setAmount(appendDto.getMember().getAmount());
                    vo.getMember().setBalance(balance);
                    break;

                default:
                    throw new InvalidRequestException();
            }

            vo.setCodeMsg(ResponseCode.SUCCESS.code);
            vo.getMember().setUsername(betDto.getMember().getUsername());

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCode.SUCCESS.code);
            vo.getMember().setUsername(betDto.getMember().getUsername());
            vo.getMember().setBalance(e.getBalance());
            if (betDto.getType().equals(TransferType.PAYOUT)) {
                vo.getMember().setAmount(betDto.getWinAmount());
            } else {
                vo.getMember().setAmount(betDto.getBetAmount().abs().negate());
            }
        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCode.INSUFFICIENT_BALANCE.code);
        } catch (InvalidRequestException | InvalidPlayerException |
                 JsonMappingException e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCode.PARAMETER_ERROR.code);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCode.OPERATION_FAILED.code);
        } finally {
            httpService.end(httpRequestLog, vo);
        }
        return vo;
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, final HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        HttpRequestLog httpRequestLogdup = new HttpRequestLog(httpRequestLog);

        // Call the service with the duplicate log
        return walletService.getBalance(traceId, gameSession, httpRequestLogdup);
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        ValidationUtils.validateRequest(dto.getMember());

        ValidationUtils.validateRequest(dto.getDetailDto());
        if ("null".equalsIgnoreCase(dto.getDetailDto().getExt())) {
            throw new InvalidRequestException();
        }
    }

    private void doVerification(BetDto dto, GameSession gameSession) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, CredentialException {
        //validate vendor username, agent vendor line, player status, and game status
        if (dto.getType().equals(TransferType.BET)) { //only check if it's bet
            validationService.validateEligibleBet(gameSession, dto.getMember().getUsername());
        }

        // Verify received token is same with credential token md5(agent+apiKey)
        String agent = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_ID);
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        ValidationUtils.isEquals(VendorService.md5Generator(agent + apiKey), dto.getToken(), CredentialException::new);
    }

    private void doValidation(AppendDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        ValidationUtils.validateRequest(dto.getMember());

        ValidationUtils.validateRequest(dto.getDetailDto());
        if (dto.getDetailDto().getParentBetId() == null) {
            throw new InvalidRequestException();
        }
        if ("null".equalsIgnoreCase(dto.getDetailDto().getExt())) {
            throw new InvalidRequestException();
        }
    }
}
