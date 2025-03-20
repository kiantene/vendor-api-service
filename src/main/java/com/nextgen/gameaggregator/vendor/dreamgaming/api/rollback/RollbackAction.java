package com.nextgen.gameaggregator.vendor.dreamgaming.api.rollback;

import com.couchbase.client.core.error.InvalidArgumentException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.dreamgaming.api.bet.AppendDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.api.bet.BetDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.TransferType;
import com.nextgen.gameaggregator.vendor.dreamgaming.dto.DetailDto;
import com.nextgen.gameaggregator.vendor.dreamgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.dreamgaming.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.CredentialException;
import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackAction {
    private final VendorService vendorService;
    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final WalletAdjustmentService walletAdjustmentService;
    private final SettledBetService settledBetService;

    public RollbackAction(VendorService vendorService, HttpService httpService,
                          VendorLineService vendorLineService,
                          WalletService walletService, WalletAdjustmentService walletAdjustmentService, SettledBetService settledBetService) {
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.walletAdjustmentService = walletAdjustmentService;
        this.settledBetService = settledBetService;
    }

    @PostMapping(path = EndPoints.CHECKNCOMPLETE)
    public ResponseVo rollback(@PathVariable("agentName") String agentName,
                               HttpServletRequest request) throws CredentialNotFoundException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo responseVo = new ResponseVo();
        RollbackDto rollbackDto = null;
        BetDto betDto;
        BigDecimal balance;
        BigDecimal appendBalance;
        GameSession gameSession;

        try {
            rollbackDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), RollbackDto.class);
            rollbackDto.setDetailDto(HttpService.convertJsonToDto(VendorService.removeLeadingZero(rollbackDto.getDetail()), DetailDto.class));

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(rollbackDto);

            // Get GameSession with username
            gameSession = vendorService.checkGameSession(traceId, rollbackDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollbackDto.getToken(), gameSession);

            //Type in (1, 6) rollback, Type in (2, 3, 5) continue to complete the transfer
            switch (rollbackDto.getType()) {
                case TransferType.BET:
                    // Retrieve the latest wallet balance from Operator
                    balance = getCurrentBalance(traceId, gameSession, httpRequestLog);
                    walletService.processRollback(rollbackDto, gameSession, vendorService, httpRequestLog);

                    responseVo.getMember().setBalance(balance);
                    responseVo.getMember().setAmount(rollbackDto.getMember().getAmount());
                    break;

                case TransferType.PAYOUT:
                    betDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), BetDto.class);
                    betDto.setDetailDto(HttpService.convertJsonToDto(VendorService.removeLeadingZero(betDto.getDetail()), DetailDto.class));

                    this.doValidation(betDto);
                    //Settle
                    ResultType updatedResultType = vendorService.calculateResultType(betDto.getBetAmount(), betDto.getWinAmount(), betDto.getJackpotAmount(), false);
                    balance = getCurrentBalance(traceId, gameSession, httpRequestLog);
                    walletService.processBetResult(traceId, gameSession, betDto, updatedResultType, vendorService, httpRequestLog);
                    responseVo.getMember().setAmount(betDto.getWinAmount());
                    responseVo.getMember().setBalance(balance);
                    break;

                case TransferType.APPEND:
                    //APPEND
                    AppendDto appendDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), AppendDto.class);
                    appendDto.setDetailDto(HttpService.convertJsonToDto(VendorService.removeLeadingZero(appendDto.getDetail()), DetailDto.class));

                    this.doValidation(appendDto);
                    // Get settle bet to calculate adjustment amount
                    SettledBet settledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(appendDto.getParentBetId(), appendDto.getRoundId(), gameSession.getVendorId(), gameSession.getVendorPlayerId());
                    appendDto.setAdjustmentAmount(appendDto.getMember().getAmount().subtract(settledBet.getWinAmount()));

                    appendBalance = walletAdjustmentService.processAdjustment(traceId, gameSession, appendDto, httpRequestLog);

                    responseVo.getMember().setAmount(appendDto.getMember().getAmount());
                    responseVo.getMember().setBalance(appendBalance.subtract(appendDto.getMember().getAmount()));
                    break;

                default:
                    throw new InvalidRequestException();
            }
            // Set response
            responseVo.setCodeMsg(ResponseCode.SUCCESS.code);
            responseVo.getMember().setUsername(rollbackDto.getMember().getUsername());

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            if (rollbackDto.getType().equals(TransferType.BET)) {
                httpService.logError(httpRequestLog, e);
                responseVo.setCodeMsg(ResponseCode.OPERATION_FAILED.code);
            } else {
                responseVo.setCodeMsg(ResponseCode.SUCCESS.code);
                responseVo.getMember().setUsername(rollbackDto.getMember().getUsername());
                if (rollbackDto.getType().equals(TransferType.PAYOUT)) {
                    responseVo.getMember().setBalance(e.getBalance().subtract(rollbackDto.getMember().getAmount()));
                } else {
                    responseVo.getMember().setBalance(e.getBalance());
                }
                responseVo.getMember().setAmount(rollbackDto.getMember().getAmount());
            }
        } catch (JsonMappingException | InvalidRequestException | InvalidPlayerException | InvalidArgumentException |
                 IllegalArgumentException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setCodeMsg(ResponseCode.PARAMETER_ERROR.code);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setCodeMsg(ResponseCode.OPERATION_FAILED.code);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        HttpRequestLog httpRequestLogdup = new HttpRequestLog(httpRequestLog);

        // Call the service with the duplicate log
        return walletService.getBalance(traceId, gameSession, httpRequestLogdup);
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        ValidationUtils.validateRequest(dto.getMember());

        ValidationUtils.validateRequest(dto.getDetailDto());
        if ("null".equalsIgnoreCase(dto.getDetailDto().getExt())) {
            throw new InvalidRequestException();
        }
    }

    private void doVerification(String token, GameSession gameSession) throws CredentialNotFoundException, CredentialException {
        // Verify received token is same with credential token md5(agent+apiKey)
        String agent = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_ID);
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);

        ValidationUtils.isEquals(VendorService.md5Generator(agent + apiKey), token, CredentialException::new);
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
