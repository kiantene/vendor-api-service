package com.nextgen.gameaggregator.vendor.gpkiconic.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorGameCodeService;
import com.nextgen.gameaggregator.vendor.gpkiconic.api.bet.BetDto;
import com.nextgen.gameaggregator.vendor.gpkiconic.constant.BetType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Getter
@Setter
public class VendorService extends BaseVendorService {
    private final VendorGameCodeService vendorGameCodeService;
    private boolean settledByBet = false;

    public VendorService(VendorGameCodeService vendorGameCodeService) {
        this.vendorGameCodeService = vendorGameCodeService;
    }

    public static long getCurrentTime() {
        return Instant.now().getEpochSecond();
    }

    public static long getMilSec() {
        return System.currentTimeMillis();
    }

    public static String trimGameCode(String gameCode) {

        String trimmedGameCode;

        // check if game code contain _stg (ignore case-sensitive)
        if (gameCode.toLowerCase().contains("_stg")) {
            // Trim value by removing _stg (ignore case-sensitive)
            trimmedGameCode = gameCode.replaceFirst("(?i)_stg$",
                    "");
        } else {
            // let trimmedCode same as gameCode
            trimmedGameCode = gameCode;
        }

        return trimmedGameCode;
    }

    public void verifyVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeService.getByVendorGameIdAndPlatformIdAndLanguageId(gameSession.getVendorGameId(),
                gameSession.getPlatformId(),
                gameSession.getLanguageId());
        if (!vendorGameCode.getBetGameCode().equals(gameId)) {
            throw new GameNotSupportedException();
        }
    }

    public VendorGameCode getVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        return vendorGameCodeService.getByBetGameCode(gameId,
                gameSession.getLanguageId(),
                gameSession.getPlatformId(),
                gameSession.getVendorId());
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }
    

    public ResultType getResultType(BetDto dto) {
        ResultType resultType = ResultType.WIN; // Default value is win

        if (dto.getMoney().compareTo(BigDecimal.ZERO) == 0 && dto.getCode().equals(BetType.POINTOUT)) {
            resultType = ResultType.END;
        }

        return resultType;
    }
}
