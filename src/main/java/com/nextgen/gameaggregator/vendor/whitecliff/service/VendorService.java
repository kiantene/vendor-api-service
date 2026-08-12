package com.nextgen.gameaggregator.vendor.whitecliff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl.PrdDto;
import com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.math.BigInteger;


@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private SettledBetService settledBetService;

    private GameSessionService gameSessionService;

    private static final String CATEGORY_LOBBY_SUBCATEGORY_CODE = "WCLIVE_LOBBY";

    @Autowired
    public VendorService(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    public static UserDto setUserDto(GameSession gameSession) {
        UserDto userDto = new UserDto();
        userDto.setName(gameSession.getVendorPlayerUsername());
        userDto.setId(BigInteger.valueOf(gameSession.getVendorPlayerId()));
        userDto.setBalance(BigDecimal.valueOf(0.00));
        userDto.setLanguage(gameSession.getVendorLanguageCode());
        userDto.setCurrency(gameSession.getVendorCurrencyCode());
        userDto.setSid(gameSession.getToken());
        return userDto;
    }

    public static PrdDto setPrdDto(GameSession gameSession, String productId, GameLaunchDataService.GameSubcategoryInfo subcategoryInfo) throws InvalidFormatException {
        PrdDto prdDto = new PrdDto();
        prdDto.setId(Integer.valueOf(productId));

        // default lobby code
        String lobbyCode = "0";

        // if is live game and not lobby game then map to table id (or category, for category-launch games)
        if (gameSession.getGameCategoryId() == 5 && !gameSession.getVendorGameCode().equals(lobbyCode)) {
            if (subcategoryInfo != null && CATEGORY_LOBBY_SUBCATEGORY_CODE.equals(subcategoryInfo.code())) {
                prdDto.setCategory(gameSession.getVendorGameCode());
            } else {
                prdDto.setTable_id(gameSession.getVendorGameCode());
            }
        } else {
            prdDto.setType(Integer.valueOf(gameSession.getVendorGameCode()));
        }

        prdDto.setIs_mobile(gameSession.getVendorPlatformCode().equals("H5"));

        // GameUrlService.formDataBuilder serializes this DTO via Gson with no @Valid anywhere on
        // the path, so PrdDto's @AssertTrue constraint never actually runs in production - this is
        // the one place that can still catch a malformed DTO before it ships to the vendor.
        if (!prdDto.isExactlyOneOfTypeTableIdCategorySet()) {
            throw new InvalidFormatException("PrdDto must have exactly one of type, table_id or category set");
        }

        return prdDto;
    }

    public static String convertMapToJson(MultiValueMap<String, String> dataMap) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(dataMap.toSingleValueMap());
        } catch (Exception e) {
            return null;
        }
    }

}
