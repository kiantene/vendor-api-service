package com.nextgen.gameaggregator.vendor.spinix.service;

import com.nextgen.gameaggregator.entity.BetInformation;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.*;
import com.nextgen.gameaggregator.vendor.spinix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.spinix.constant.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;

    public static String getSignature(Map<String, Object> args, String signatureKey) {
        StringBuilder value = new StringBuilder(signatureKey);
        Map<String, Object> keyObject = _getKeyValueFromObject(args, "");
        List<String> keys = new ArrayList<>(keyObject.keySet());
        keys = keys.stream().filter(item -> !item.equals("signature")).collect(Collectors.toList());
        Collections.sort(keys);
        for (String key : keys) {
            value.append("&").append(key).append("=").append(keyObject.get(key));
        }
        return DigestUtils.md5Hex(value.toString());
    }

    public static Map<String, Object> _getKeyValueFromObject(Map<String, Object> args, String prefixKey) {
        Map<String, Object> result = new HashMap<>();
        for (String key : args.keySet()) {
            if (args.get(key) == null) {
                continue;
            }

            if (!(args.get(key) instanceof Map) && !(args.get(key) instanceof ArrayList)) {
                String resultKey = "";
                if (!prefixKey.isEmpty()) {
                    resultKey = prefixKey + ".";
                }
                resultKey += key;
                result.put(resultKey, args.get(key));
                continue;
            }

            if (args.get(key) instanceof Map) {
                if (!((Map) args.get(key)).isEmpty()) {
                    String nestedPrefixKey = key;
                    if (!prefixKey.isEmpty()) {
                        nestedPrefixKey = prefixKey + "." + key;
                    }
                    Map<String, Object> nestedResult = _getKeyValueFromObject((Map<String, Object>) args.get(key), nestedPrefixKey);
                    result.putAll(nestedResult);
                }
            }

            if (args.get(key) instanceof ArrayList data) {
                if (!data.isEmpty()) {

                    for (int i = 0; i < data.size(); i++) {
                        String nestedPrefixKey = key + "." + i;
                        Map<String, Object> nestedResult = _getKeyValueFromObject((Map<String, Object>) data.get(i), nestedPrefixKey);
                        result.putAll(nestedResult);
                    }
                }
            }

        }
        return result;
    }

    public static void validateSignature(String signature, Map<String, Object> body, String signatureKey) throws InvalidVendorLineException {
        String sign = getSignature(body, signatureKey);
        if (!signature.equals(sign)) throw new InvalidVendorLineException();
    }

    @Override
    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {
        return betInfo.getEffectiveTurnover();
    }

    public Map<String, RoundPayoutTransactionDto> getTransactions(RoundPayoutDto dto) {
        Map<String, RoundPayoutTransactionDto> transactions = new HashMap<>();

        for (RoundPayoutTransactionDto txnDto : dto.getTransactionList()) {
            transactions.put(txnDto.getType(), txnDto);
        }

        return transactions;
    }

    public boolean isBetTransactionOnly(Map<String, RoundPayoutTransactionDto> txnMap) {
        return txnMap.containsKey(TransactionType.BET);
    }

    public boolean isWinTransactionOnly(Map<String, RoundPayoutTransactionDto> txnMap) {
        return txnMap.containsKey(TransactionType.WIN);
    }

    public boolean isBetAndWin(Map<String, RoundPayoutTransactionDto> txnMap) {
        return (txnMap.containsKey(TransactionType.BET) && txnMap.containsKey(TransactionType.WIN));
    }

    public boolean isCancelBet(Map<String, RoundPayoutTransactionDto> txnMap) {
        return txnMap.containsKey(TransactionType.CANCEL_BET);
    }

    public GameSession getGameSession(RoundPayoutDto dto) throws AuthenticationException {

        GameSession gameSession;

        // user token could be null. Get latest game session when user token is null. Otherwise verify user token
        if (dto.getUserToken() == null) {
            // TODO: vendor has no intention to send user_token with value for fish game's win transaction record. Use user token from last game session first
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getUserId(), dto.getGameId());
        } else {
            // Verify session token
            gameSession = gameSessionService.verifyToken(dto.getUserToken());
        }

        return gameSession;
    }

    public RoundPayoutVo getCurrentBalanceResponseVo(HttpRequestLog httpRequestLog, String traceId, GameSession gameSession, RoundPayoutDto dto) {

        RoundPayoutDataWalletVo roundPayoutDataWalletVo = new RoundPayoutDataWalletVo();
        RoundPayoutDataVo roundPayoutDataVo = new RoundPayoutDataVo();
        RoundPayoutVo roundPayoutVo = new RoundPayoutVo();
        RoundPayoutErrorVo roundPayoutErrorVo = new RoundPayoutErrorVo();
        Integer status = HttpStatus.SC_OK;

        try {

            // Set req id
            String reqId = dto.getReqId();
            roundPayoutVo.setReqId(reqId);

            BigDecimal balance = walletService.getBalance(traceId, gameSession);
            roundPayoutDataWalletVo.setBalance(balance);

            // Set Currency + RoundPayoutDataWalletVo + Status
            roundPayoutDataWalletVo.setCurrency(gameSession.getVendorCurrencyCode());
            roundPayoutDataWalletVo.setBalance(balance);
            roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);
            roundPayoutVo.setStatus(status);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (Exception exception) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, exception);
        } finally {
            if (roundPayoutVo.getStatus() == HttpStatus.SC_OK) {
                roundPayoutVo.setData(roundPayoutDataVo);
            } else {
                roundPayoutErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(roundPayoutErrorVo.getCode()));
                roundPayoutVo.setError(roundPayoutErrorVo);
            }
        }

        return roundPayoutVo;
    }
}
