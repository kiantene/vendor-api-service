package com.nextgen.gameaggregator.vendor.cg.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidVendorException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.repository.ga.writer.VendorRepository;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.cg.api.record.RecordDto;
import com.nextgen.gameaggregator.vendor.cg.api.record.RecordVo;
import com.nextgen.gameaggregator.vendor.cg.constant.Action;
import com.nextgen.gameaggregator.vendor.cg.constant.Format;
import com.nextgen.gameaggregator.vendor.cg.constant.RecordStatus;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VendorService extends BaseVendorService {

    @Autowired
    private static VendorLineService vendorLineService;
    @Autowired
    private VendorRepository vendorRepository;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private UnsettledBetService unsettledBetService;

    //public static byte[] ivbyte = Base64.decode("YRFxDqdDDF5ExcQN5yFzUA==");
    //public static byte[] keyBytes = Base64.decode("XXhGuwr3cyl6YnIYJ4gbNrZWctZ7b2rRd4QkExoOZ7k=");

    public static String encrypt(String jsonString, String iv, String key) throws DataLengthException, IllegalStateException {

        try {
            byte[] ivbyte = Base64.decode(iv);
            byte[] keyBytes = Base64.decode(key);

            AES_CBC aes = new AES_CBC(keyBytes, ivbyte);

            aes.InitCiphers();

            byte[] encryptedJsonString = null;
            encryptedJsonString = aes.CBCEncrypt(jsonString.getBytes());
            String encodedJson = new String(Base64.encode(encryptedJsonString));
            return encodedJson;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decryptResponse(String response, String iv, String key) {
        try {
            byte[] ivbyte = Base64.decode(iv);
            byte[] keyBytes = Base64.decode(key);

            AES_CBC aes = new AES_CBC(keyBytes, ivbyte);

            aes.InitCiphers();


            byte[] decryptData = Base64.decode(response);
            byte[] decryptMessage;
            decryptMessage = aes.CBCDecrypt(decryptData);
            return new String(decryptMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public static Map<String, Object> convertToHashMap(MultiValueMap<String, String> multiValueMap) {
        Map<String, Object> hashMap = new HashMap<>();

        // Iterate over entries in the MultiValueMap
        for (Map.Entry<String, List<String>> entry : multiValueMap.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            // Convert the list of values into an Object, e.g., by selecting the first value
            Object value = (values != null && !values.isEmpty()) ? (Object) values.get(0) : null;
            hashMap.put(key, value);
        }

        return hashMap;
    }

    public static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Handle decoding exception as needed
            e.printStackTrace();
            return null;
        }
    }

    public static String returnTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.ofHours(8));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Format.DATE_TIME_FORMAT);
        String formattedDateTime = now.format(formatter);

        formattedDateTime = formattedDateTime.replace("Z", "+08:00");

        return formattedDateTime;
    }

    public static String unixTimestampToDateTime(Long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat(Format.DATE_TIME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String formattedDateTime = sdf.format(date);

        formattedDateTime = formattedDateTime.replace("Z", "+08:00");

        return formattedDateTime;
    }

    public static Boolean checkEmptyString(Map checkMap) {
        return checkMap.containsValue("%20");
    }

    public Vendor findVendorByCode(String vendorCode) throws InvalidVendorException {
        Vendor vendor = vendorRepository.findByCode(vendorCode);
        Optional.ofNullable(vendor).orElseThrow(InvalidVendorException::new);
        return vendor;
    }

    public RecordVo settledBetIdempotentCheck(GameSession gameSession, RecordDto dto) {

        Long vendorPlayerId = gameSession.getVendorPlayerId();
        SettledBet settledBet;
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;

        RecordVo responseVo = new RecordVo();

        try {

            settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, dto.getMtcode());

            if (settledBet != null) { // duplicate request found in settled_bet
                Integer operatorStatus = settledBet.getOperatorStatus();
                // throw idempotent exception if status is processing or success
                if (operatorStatus.equals(operatorStatusProcessing)) {
                    responseVo.getData().setTransaction_id(null);
                    responseVo.getData().setAction(Action.ENDROUND);
                    responseVo.getData().getTarget().setAccount(dto.getAccountId());
                    responseVo.getData().getBalance().setBefore(BigDecimal.ZERO);
                    responseVo.getData().getBalance().setAfter(BigDecimal.ZERO);
                    responseVo.getData().getStatus().setCreatetime(null);
                    responseVo.getData().getStatus().setEndtime(null); //still processing so no end time yet
                    responseVo.getData().getStatus().setStatus(RecordStatus.PENDING); //still processing
                    responseVo.getData().getStatus().setMessage("Bet result still processing");
                    responseVo.getData().setCurrency(gameSession.getVendorCurrencyCode());
                    responseVo.getData().getIncident().setMtcode(settledBet.getExternalTransactionId());
                    responseVo.getData().getIncident().setAmount(null);
                    responseVo.getData().getIncident().setEventtime(null);
                    responseVo.setChannelId(dto.getChannelId());
                    responseVo.setErrorCode(com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes.SUCCESS);
                    responseVo.setReturnTime(returnTime());

                    return responseVo;

                } else if (operatorStatus.equals(operatorStatusSuccess) && Objects.equals(settledBet.getStatus(), BetStatus.SETTLED.code)) {
                    responseVo.getData().setTransaction_id(settledBet.getBetId());
                    responseVo.getData().setAction(Action.ENDROUND);
                    responseVo.getData().getTarget().setAccount(dto.getAccountId());
                    responseVo.getData().getBalance().setBefore(settledBet.getBalance().subtract(settledBet.getWinLoss().add(settledBet.getBetAmount())).setScale(2, RoundingMode.DOWN));
                    responseVo.getData().getBalance().setAfter(settledBet.getBalance().setScale(2, RoundingMode.DOWN));
                    responseVo.getData().getStatus().setCreatetime(unixTimestampToDateTime(settledBet.getCreateTime()));
                    responseVo.getData().getStatus().setEndtime(unixTimestampToDateTime(settledBet.getResultTime()));
                    responseVo.getData().getStatus().setStatus(checkBetStatus(settledBet.getStatus()));
                    responseVo.getData().getStatus().setMessage("Success");
                    responseVo.getData().setCurrency(gameSession.getVendorCurrencyCode());
                    responseVo.getData().getIncident().setMtcode(settledBet.getExternalTransactionId());
                    responseVo.getData().getIncident().setAmount(settledBet.getWinAmount().setScale(2, RoundingMode.DOWN));
                    responseVo.getData().getIncident().setEventtime(unixTimestampToDateTime(settledBet.getVendorSettleTime()));
                    responseVo.setChannelId(dto.getChannelId());
                    responseVo.setErrorCode(com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes.SUCCESS);
                    responseVo.setReturnTime(returnTime());

                    return responseVo;
                } else if (operatorStatus.equals(operatorStatusSuccess) && Objects.equals(settledBet.getStatus(), BetStatus.REFUNDED.code)) { //check refund
                    responseVo.getData().setTransaction_id(settledBet.getBetId());
                    responseVo.getData().setAction(Action.BET); //confirmed with vendor
                    responseVo.getData().getTarget().setAccount(dto.getAccountId());
                    responseVo.getData().getBalance().setBefore(settledBet.getBalance().subtract(settledBet.getBetAmount()).setScale(2, RoundingMode.DOWN)); //confirmed with vendor to show value
                    responseVo.getData().getBalance().setAfter(settledBet.getBalance().setScale(2, RoundingMode.DOWN)); //confirmed with vendor to show value
                    responseVo.getData().getStatus().setCreatetime(unixTimestampToDateTime(settledBet.getCreateTime()));
                    responseVo.getData().getStatus().setEndtime(unixTimestampToDateTime(settledBet.getResultTime()));
                    responseVo.getData().getStatus().setStatus(checkBetStatus(settledBet.getStatus()));
                    responseVo.getData().getStatus().setMessage("Refunded");
                    responseVo.getData().setCurrency(gameSession.getVendorCurrencyCode());
                    responseVo.getData().getIncident().setMtcode(settledBet.getExternalTransactionId());
                    responseVo.getData().getIncident().setAmount(settledBet.getBetAmount().setScale(2, RoundingMode.DOWN));
                    responseVo.getData().getIncident().setEventtime(unixTimestampToDateTime(settledBet.getVendorSettleTime()));
                    responseVo.setChannelId(dto.getChannelId());
                    responseVo.setErrorCode(com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes.SUCCESS);
                    responseVo.setReturnTime(returnTime());

                    return responseVo;
                } else { // when bet result found and operator status is error
                    //no action
                }
            }
        } catch (BetNotFoundException betNotFoundException) {
            //no action
        }

        return null;
    }

    public RecordVo unsettledBetIdempotentCheck(GameSession gameSession, RecordDto dto) {

        Integer vendorId = gameSession.getVendorId();
        UnsettledBet unsettledBet;
        Integer operatorStatusProcessing = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        Integer operatorStatusSuccess = ResponseCodes.Status.SC_OK.code;
        RecordVo responseVo = new RecordVo();

        try {

            unsettledBet = unsettledBetService.getByVendorIdAndExternalTransactionId(vendorId, dto.getMtcode());

            if (unsettledBet != null) { // duplicate request found in settled_bet
                Integer operatorStatus = unsettledBet.getOperatorStatus();
                // throw idempotent exception if status is processing or success
                if (operatorStatus.equals(operatorStatusProcessing)) {
                    responseVo.getData().setTransaction_id(null);
                    responseVo.getData().setAction(Action.BET);
                    responseVo.getData().getTarget().setAccount(dto.getAccountId());
                    responseVo.getData().getBalance().setBefore(BigDecimal.ZERO);
                    responseVo.getData().getBalance().setAfter(BigDecimal.ZERO);
                    responseVo.getData().getStatus().setCreatetime(null);
                    responseVo.getData().getStatus().setEndtime(null); //still processing so no end time yet
                    responseVo.getData().getStatus().setStatus(RecordStatus.PENDING); //still processing
                    responseVo.getData().getStatus().setMessage("Bet still processing");
                    responseVo.getData().setCurrency(gameSession.getVendorCurrencyCode());
                    responseVo.getData().getIncident().setMtcode(unsettledBet.getExternalTransactionId());
                    responseVo.getData().getIncident().setAmount(null);
                    responseVo.getData().getIncident().setEventtime(null);
                    responseVo.setChannelId(dto.getChannelId());
                    responseVo.setErrorCode(com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes.SUCCESS);
                    responseVo.setReturnTime(returnTime());

                    return responseVo;

                } else if (operatorStatus.equals(operatorStatusSuccess)) {
                    responseVo.getData().setTransaction_id(unsettledBet.getBetId());
                    responseVo.getData().setAction(Action.BET);
                    responseVo.getData().getTarget().setAccount(dto.getAccountId());
                    responseVo.getData().getBalance().setBefore(unsettledBet.getBalance().add(unsettledBet.getBetAmount()).setScale(2, RoundingMode.DOWN));
                    responseVo.getData().getBalance().setAfter(unsettledBet.getBalance().setScale(2, RoundingMode.DOWN));
                    responseVo.getData().getStatus().setCreatetime(unixTimestampToDateTime(unsettledBet.getCreateTime()));
                    responseVo.getData().getStatus().setEndtime(unixTimestampToDateTime(unsettledBet.getResultTime()));
                    responseVo.getData().getStatus().setStatus(checkBetStatus(unsettledBet.getStatus()));
                    responseVo.getData().getStatus().setMessage("Success");
                    responseVo.getData().setCurrency(gameSession.getVendorCurrencyCode());
                    responseVo.getData().getIncident().setMtcode(unsettledBet.getExternalTransactionId());
                    responseVo.getData().getIncident().setAmount(unsettledBet.getBetAmount().setScale(2, RoundingMode.DOWN));
                    responseVo.getData().getIncident().setEventtime(unixTimestampToDateTime(unsettledBet.getVendorSettleTime()));
                    responseVo.setChannelId(dto.getChannelId());
                    responseVo.setErrorCode(com.nextgen.gameaggregator.vendor.cg.constant.ResponseCodes.SUCCESS);
                    responseVo.setReturnTime(returnTime());

                    return responseVo;

                } else { // when bet result found and operator status is error
                    //no action
                }
            }
        } catch (BetNotFoundException betNotFoundException) {
            //no action
        }
        return null;
    }

    public String checkBetStatus(Integer statusCode) {
        switch (statusCode) {
            case 0, 1:
                return RecordStatus.SUCCESS;
            case 2:
                return RecordStatus.FAILED;
            case 3:
                return RecordStatus.REFUND;
            default:
                return null;
        }
    }


}
