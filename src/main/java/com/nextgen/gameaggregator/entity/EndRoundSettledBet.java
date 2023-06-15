package com.nextgen.gameaggregator.entity;

import com.nextgen.gameaggregator.enums.BetStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@NoArgsConstructor
public class EndRoundSettledBet extends BetInformation {

    private String agentPlayerUsername;
    private String currencyCode;
    private String gameCode;
    private Integer gaResultType;

    //temp logger, will remove
    private static final Logger logger = LoggerFactory.getLogger(EndRoundSettledBet.class);

    public EndRoundSettledBet(SettledBet settledBet, String agentPlayerUsername, String currencyCode, String gameCode, Integer gaResultType){

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(settledBet, this);

        //temp log, will remove
        if(this.getGameSessionToken() == null){
            logger.warn("missing token roundId = "+settledBet.getRoundId()+" betId = "+settledBet.getVendorBetId());
        }

        this.setStatus(BetStatus.SETTLED.code);
        this.agentPlayerUsername = agentPlayerUsername;
        this.currencyCode = currencyCode;
        this.gameCode = gameCode;
        this.gaResultType = gaResultType;
    }

}
