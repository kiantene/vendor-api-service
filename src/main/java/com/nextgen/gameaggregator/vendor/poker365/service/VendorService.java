package com.nextgen.gameaggregator.vendor.poker365.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class VendorService extends BaseVendorService {


    private VendorLineService vendorLineService;
    private GameSessionService gameSessionService;
    private VendorGameCodeService vendorGameCodeService;
    private WalletService walletService;
    private HttpService httpService;

    private boolean rejectSettleAfterRollback = true;

    @Autowired
    private VendorService(VendorLineService vendorLineService, GameSessionService gameSessionService, VendorGameCodeService vendorGameCodeService, WalletService walletService, HttpService httpService) {
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.vendorGameCodeService = vendorGameCodeService;
        this.walletService = walletService;
        this.httpService = httpService;
    }

    public static <T> T convertQueryStringToDtoUrlDecode(String queryString, Class<T> objectClass) throws InvalidRequestException {
        Map<String, Object> queryParameterMap = new HashMap<>();

        // TODO: To review on this exception handling
        try {
            queryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String[] fields = queryString.split("&");

        for (String field : fields) {
            String[] kv = field.split("=");
            if (kv.length == 2) {
                Object currentValue = queryParameterMap.get(kv[0]);
                if (currentValue == null) {
                    queryParameterMap.put(kv[0], kv[1]);
                } else if (currentValue instanceof String) {
                    String[] values = {(String) currentValue, kv[1]};
                    queryParameterMap.put(kv[0], values);
                } else if (currentValue instanceof String[]) {
                    String[] values = (String[]) currentValue;
                    Integer newLength = values.length + 1;
                    String[] newValues = Arrays.copyOf(values, newLength);
                    newValues[newLength - 1] = kv[1];
                    queryParameterMap.put(kv[0], newValues);
                }
            }
        }
        ObjectMapper mapper = new ObjectMapper();

        T object;
        try {
            object = mapper.convertValue(queryParameterMap, objectClass);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException();
        }

        return object;
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        //Temporary only BGAMING, SpadeGaming, EvoNetent need to accept cancel request
        return false;
    }


    public void validateExternalGameSessionId(String externalGameSessionId) throws InvalidRequestException {
        if (!externalGameSessionId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new InvalidRequestException();
        }
    }

    public <T> GameSession getGameSession(T dto)
            throws
            AuthenticationException,
            InvalidRequestException,
            NoSuchMethodException,
            InvocationTargetException,
            IllegalAccessException {

        GameSession gameSession;

        Method getExternalGameSessionIdMethod = dto.getClass().getMethod("getExternalGameSessionId");
        String externalGameSessionId = (String) getExternalGameSessionIdMethod.invoke(dto);

        if (externalGameSessionId == null || externalGameSessionId.isEmpty()) {
            Method getExternalId = dto.getClass().getMethod("getExternalId");
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername((String) getExternalId.invoke(dto));

        } else {
            // validate extern game session id
            this.validateExternalGameSessionId(externalGameSessionId);

            // Verify session token
            gameSession = gameSessionService.verifyToken(externalGameSessionId);

        }

        return gameSession;

    }
}
