package com.nextgen.gameaggregator.operator.sport.updatebet;

import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.sport.SportsBaseAction;
import org.springframework.stereotype.Service;

@Service
public class SportUpdateBetAction extends SportsBaseAction {

    public SportUpdateBetAction() {
        this.endpoint = EndPoints.SPORT_UPDATE_BET;
        this.requestType = this.getClass().getSimpleName();
    }
}
