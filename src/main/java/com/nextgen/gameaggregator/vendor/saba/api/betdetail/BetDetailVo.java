package com.nextgen.gameaggregator.vendor.saba.api.betdetail;

import com.google.gson.annotations.SerializedName;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.operator.constant.Sport;
import com.nextgen.gameaggregator.operator.transactions.detail.MatchDetailData;
import com.nextgen.gameaggregator.operator.transactions.detail.SportBetDetailVo;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Collections;
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

    private VendorLanguageCode vendorLanguageCode;

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
        return Optional.ofNullable(this.getData().getBetDetails().get(0).getParlayData())
                .map(parlayData -> this.getData().getBetDetails().get(0).getParlayRefNo())
                .orElse(this.getData().getBetDetails().get(0).getTransId());
    }

    @Override
    public Long getTransactionTime() {
        return VendorService.convertToUnixTimestamp(this.getData().getBetDetails().get(0).getSettlementTime(), "yyyy-MM-dd'T'HH:mm:ss.SS");
    }

    @Override
    public List<MatchDetailData> getMatchDetail() {
        return Optional.ofNullable(this.getData().getBetDetails().get(0).getParlayData())
                .map(this::getParlayDetail)
                .orElseGet(() -> this.getNormalMatchDetail(this.getData().getBetDetails().get(0)));
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
    public Boolean getIsCashout() {
        return Optional.ofNullable(this.getData().getBetDetails().get(0).getTicketExtraStatus()).map(value -> value.equalsIgnoreCase("cashout")).orElse(Boolean.FALSE);
    }

    public List<MatchDetailData> getNormalMatchDetail(BetDetailsDto betDetailsDto) {
        MatchDetailData matchDetailData = new MatchDetailData();
        matchDetailData.setMatchName(VendorService.getNameByLang(this.getVendorLanguageCode(), betDetailsDto.getLeaguename()));
        matchDetailData.setMatchDate(VendorService.convertToUnixTimestamp(betDetailsDto.getMatchDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));
        matchDetailData.setHomeTeamName(VendorService.getNameByLang(this.getVendorLanguageCode(), betDetailsDto.getHometeamname()));
        matchDetailData.setAwayTeamName(VendorService.getNameByLang(this.getVendorLanguageCode(), betDetailsDto.getAwayteamname()));
        matchDetailData.setHomeTeamScore(betDetailsDto.getHomeScore());
        matchDetailData.setAwayTeamScore(betDetailsDto.getAwayScore());
        matchDetailData.setBetTypeName(VendorService.getNameByLang(this.getVendorLanguageCode(), betDetailsDto.getBetTypeName()));
        matchDetailData.setBetTeam(this.getTeam(betDetailsDto.getBetTeam()));
        matchDetailData.setHandicap(betDetailsDto.getHdp());
        matchDetailData.setOdds(betDetailsDto.getOdds());
        matchDetailData.setBetStatus(this.getBetStatus(betDetailsDto.getTicketStatus()));
        matchDetailData.setSettleDate(VendorService.convertToUnixTimestamp(betDetailsDto.getWinlostDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));
        return Collections.singletonList(matchDetailData);
    }

    public List<MatchDetailData> getParlayDetail(List<ParlayDataDto> parlayDataDtoList) {

        List<MatchDetailData> parlayDetailDataList = new LinkedList<>();

        for (ParlayDataDto parlayDataDto : parlayDataDtoList) {

            MatchDetailData sportParlayDetailData = new MatchDetailData();

            sportParlayDetailData.setMatchName(VendorService.getNameByLang(this.getVendorLanguageCode(), parlayDataDto.getLeaguename()));
            sportParlayDetailData.setMatchDate(VendorService.convertToUnixTimestamp(parlayDataDto.getMatchDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));
            sportParlayDetailData.setHomeTeamName(VendorService.getNameByLang(this.getVendorLanguageCode(), parlayDataDto.getHometeamname()));
            sportParlayDetailData.setAwayTeamName(VendorService.getNameByLang(this.getVendorLanguageCode(), parlayDataDto.getAwayteamname()));
            sportParlayDetailData.setHomeTeamScore(parlayDataDto.getHomeScore());
            sportParlayDetailData.setAwayTeamScore(parlayDataDto.getAwayScore());
            sportParlayDetailData.setBetTypeName(VendorService.getNameByLang(this.getVendorLanguageCode(), parlayDataDto.getBettypename()));
            sportParlayDetailData.setBetTeam(this.getTeam(parlayDataDto.getBetTeam()));
            sportParlayDetailData.setHandicap(parlayDataDto.getHdp());
            sportParlayDetailData.setOdds(parlayDataDto.getOdds());
            sportParlayDetailData.setBetStatus(this.getBetStatus(parlayDataDto.getTicketStatus()));
            sportParlayDetailData.setSettleDate(VendorService.convertToUnixTimestamp(parlayDataDto.getWinlostDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));

            parlayDetailDataList.add(sportParlayDetailData);
        }
        return parlayDetailDataList;
    }

    private String getBetStatus(String value) {
        return switch (value.toUpperCase()) {
            case "HALF WON" -> Sport.BetStatus.HALF_WIN.value;
            case "HALF LOSE" -> Sport.BetStatus.HALF_LOSE.value;
            case "WON" -> Sport.BetStatus.WIN.value;
            case "LOSE" -> Sport.BetStatus.LOSE.value;
            case "DRAW" -> Sport.BetStatus.DRAW.value;
            case "VOID" -> Sport.BetStatus.CANCELLED.value;
            case "RUNNING" -> Sport.BetStatus.RUNNING.value;
            case "REJECT" -> Sport.BetStatus.REJECTED.value;
            case "REFUND" -> Sport.BetStatus.REFUNDED.value;
            default -> Sport.BetStatus.PENDING.value;
        };
    }

    private String getTeam(String value) {
        return switch (value.toUpperCase()) {
            case "A" -> Sport.BetTeam.TEAM_AWAY.value;
            case "H" -> Sport.BetTeam.TEAM_HOME.value;
            default -> Sport.BetTeam.NO_TEAM.value;
        };
    }

//    private String getValue(String value) {
//        return Optional.ofNullable(value)
////                .filter(val -> !val.isEmpty())
//                .filter(val -> !val.isBlank())
//                .orElse(null);
//    }
}
