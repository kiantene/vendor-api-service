package com.nextgen.gameaggregator.vendor.saba.api.betdetail;

import com.google.gson.annotations.SerializedName;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
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
    public MatchDetailData getMatchDetail() {

        Optional<List<ParlayDataDto>> parlayDataDtoList = Optional.ofNullable(this.getData().getBetDetails().get(0).getParlayData());
        BetDetailsDto betDetailsDto = this.getData().getBetDetails().get(0);

        if (parlayDataDtoList.isPresent()) {
            return null;
        }

        MatchDetailData matchDetailData = new MatchDetailData();
        matchDetailData.setMatchName(VendorService.getNameByLang(this.getVendorLanguageCode(), betDetailsDto.getLeaguename()));
        matchDetailData.setMatchDate(VendorService.convertToUnixTimestamp(betDetailsDto.getMatchDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));
        matchDetailData.setHomeTeamName(VendorService.getNameByLang(this.getVendorLanguageCode(), betDetailsDto.getHometeamname()));
        matchDetailData.setAwayTeamName(VendorService.getNameByLang(this.getVendorLanguageCode(), betDetailsDto.getAwayteamname()));
        matchDetailData.setHomeTeamScore(betDetailsDto.getHomeScore());
        matchDetailData.setAwayTeamScore(betDetailsDto.getAwayScore());
        matchDetailData.setBetTypeName(VendorService.getNameByLang(this.getVendorLanguageCode(), betDetailsDto.getBetTypeName()));
        return matchDetailData;
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

        Optional<List<ParlayDataDto>> parlayDataDtoList = Optional.ofNullable(this.getData().getBetDetails().get(0).getParlayData());

        if (parlayDataDtoList.isEmpty()) {
            return null;
        }

        List<SportParlayDetailData> parlayDetailDataList = new LinkedList<>();

        for (ParlayDataDto parlayDataDto : parlayDataDtoList.get()) {

            SportParlayDetailData sportParlayDetailData = new SportParlayDetailData();

            sportParlayDetailData.setMatchName(VendorService.getNameByLang(this.getVendorLanguageCode(), parlayDataDto.getLeaguename()));
            sportParlayDetailData.setMatchDate(VendorService.convertToUnixTimestamp(parlayDataDto.getMatchDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));
            sportParlayDetailData.setHomeTeamName(VendorService.getNameByLang(this.getVendorLanguageCode(), parlayDataDto.getHometeamname()));
            sportParlayDetailData.setAwayTeamName(VendorService.getNameByLang(this.getVendorLanguageCode(), parlayDataDto.getAwayteamname()));
            sportParlayDetailData.setHomeTeamScore(parlayDataDto.getHomeScore());
            sportParlayDetailData.setAwayTeamScore(parlayDataDto.getAwayScore());
            sportParlayDetailData.setBetTypeName(VendorService.getNameByLang(this.getVendorLanguageCode(), parlayDataDto.getBettypename()));
            sportParlayDetailData.setOdds(parlayDataDto.getOdds());
            sportParlayDetailData.setBetStatus(this.getBetStatus(parlayDataDto.getTicketStatus()));
            sportParlayDetailData.setSettleDate(VendorService.convertToUnixTimestamp(parlayDataDto.getWinlostDatetime(), "yyyy-MM-dd'T'HH:mm:ss"));

            parlayDetailDataList.add(sportParlayDetailData);
        }
        return parlayDetailDataList;
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
