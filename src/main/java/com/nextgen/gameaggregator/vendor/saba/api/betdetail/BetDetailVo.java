package com.nextgen.gameaggregator.vendor.saba.api.betdetail;

import com.google.gson.annotations.SerializedName;
import com.nextgen.gameaggregator.operator.constant.SportBetStatus;
import com.nextgen.gameaggregator.operator.transactions.detail.MatchDetailData;
import com.nextgen.gameaggregator.operator.transactions.detail.SportBetDetailVo;
import com.nextgen.gameaggregator.operator.transactions.detail.SportParlayDetailData;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Data
public class BetDetailVo implements SportBetDetailVo {

    @SerializedName("error_code")
    private String errorCode;

    private String message;

    @SerializedName("Data")
    private DataDto data;

    @Override
    public String getBetNumber() {
        return this.getData().getBetDetails().get(0).getTransId();
    }

    @Override
    public String getVendorUsername() {
        return this.getData().getBetDetails().get(0).getVendorMemberId();
    }

    @Override
    public String getReferenceNumber() {
        String referenceNumber;
        if (Optional.ofNullable(this.getData().getBetDetails().get(0).getParlayData()).isPresent()) {
            referenceNumber = this.getData().getBetDetails().get(0).getParlayRefNo();
        } else {
            referenceNumber = this.getData().getBetDetails().get(0).getTransId();
        }
        return referenceNumber;
    }

    @Override
    public Long getTransactionTime() {
        return VendorService.convertToUnixTimestamp(this.getData().getBetDetails().get(0).getSettlementTime(), "yyyy-MM-dd'T'HH:mm:ss.SS");
    }

    @Override
    public MatchDetailData getMatchDetail() {

        if (Optional.ofNullable(this.getData().getBetDetails().get(0).getParlayData()).isPresent()) {
            return null;
        } else {
            MatchDetailData matchDetailData = new MatchDetailData();
            matchDetailData.setMatchName(this.getData().getBetDetails().get(0).getLeaguename().get(0).getName());
            matchDetailData.setMatchDate(VendorService.convertToUnixTimestamp(this.getData().getBetDetails().get(0).getMatchDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));
            matchDetailData.setHomeTeamName(this.getData().getBetDetails().get(0).getHometeamname().get(0).getName());
            matchDetailData.setAwayTeamName(this.getData().getBetDetails().get(0).getAwayteamname().get(0).getName());
            matchDetailData.setHomeTeamScore(this.getData().getBetDetails().get(0).getHomeScore());
            matchDetailData.setAwayTeamScore(this.getData().getBetDetails().get(0).getAwayScore());
            return matchDetailData;
        }
    }

    @Override
    public String getOdds() {
        return this.getData().getBetDetails().get(0).getOdds();
    }

    @Override
    public BigDecimal getStake() {
        return new BigDecimal(this.getData().getBetDetails().get(0).getStake());
    }

    @Override
    public BigDecimal getWinLoss() {
        return new BigDecimal(this.getData().getBetDetails().get(0).getWinlostAmount());
    }

    @Override
    public String getStatus() {
        return this.getBetStatus(this.getData().getBetDetails().get(0).getTicketStatus());
    }

    @Override
    public List<SportParlayDetailData> getParlayDetail() {

        if (Optional.ofNullable(this.getData().getBetDetails().get(0).getParlayData()).isEmpty()) {
            return null;
        } else {
            List<SportParlayDetailData> parlayDetailDataList = new LinkedList<>();

            for (ParlayDataDto parlayDataDto : this.getData().getBetDetails().get(0).getParlayData()) {

                SportParlayDetailData sportParlayDetailData = new SportParlayDetailData();

                sportParlayDetailData.setMatchName(parlayDataDto.getLeaguename().get(0).getName());
                sportParlayDetailData.setMatchDate(VendorService.convertToUnixTimestamp(parlayDataDto.getMatchDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));
                sportParlayDetailData.setHomeTeamName(parlayDataDto.getHometeamname().get(0).getName());
                sportParlayDetailData.setAwayTeamName(parlayDataDto.getAwayteamname().get(0).getName());
                sportParlayDetailData.setHomeTeamScore(parlayDataDto.getHomeScore());
                sportParlayDetailData.setAwayTeamScore(parlayDataDto.getAwayScore());
                sportParlayDetailData.setBetTypeName(parlayDataDto.getBettypename().get(0).getName());
                sportParlayDetailData.setOdds(parlayDataDto.getOdds());
                sportParlayDetailData.setBetStatus(this.getBetStatus(parlayDataDto.getTicketStatus()));
                sportParlayDetailData.setSettleDate(VendorService.convertToUnixTimestamp(parlayDataDto.getWinlostDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));

                parlayDetailDataList.add(sportParlayDetailData);
            }
            return parlayDetailDataList;
        }
    }

    private String getBetStatus(String value) {
        return switch (value.toUpperCase()) {
            case "HALF WON" -> SportBetStatus.BetStatus.HALF_WIN.value;
            case "HALF LOSE" -> SportBetStatus.BetStatus.HALF_LOSE.value;
            case "WON" -> SportBetStatus.BetStatus.WIN.value;
            case "LOSE" -> SportBetStatus.BetStatus.LOSE.value;
            case "DRAW" -> SportBetStatus.BetStatus.DRAW.value;
            case "VOID" -> SportBetStatus.BetStatus.CANCELLED.value;
            case "RUNNING" -> SportBetStatus.BetStatus.RUNNING.value;
            case "REJECT" -> SportBetStatus.BetStatus.REJECTED.value;
            case "REFUND" -> SportBetStatus.BetStatus.REFUNDED.value;
            default -> SportBetStatus.BetStatus.PENDING.value;
        };
    }
}
