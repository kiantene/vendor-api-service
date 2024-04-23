package com.nextgen.gameaggregator.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.enums.DateRangeType;
import com.nextgen.gameaggregator.enums.HotTopGameType;
import com.nextgen.gameaggregator.exception.InvalidVendorException;
import com.nextgen.gameaggregator.operator.game.hottoplist.GameHotTopData;
import com.nextgen.gameaggregator.operator.game.hottoplist.GameHotTopListDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameHotTopListService {

    @PersistenceContext(unitName = "mysqlPersistenceUnitGaReader")
    private EntityManager entityManager;
    @Value("${image.gameurl}")
    private String imageUrl;
    @Autowired
    private VendorService vendorService;

    public List<GameHotTopData> getHotTopGameList(GameHotTopListDto dto, Integer currencyId, Boolean isTestEnvironment) throws InvalidVendorException {
        List<GameHotTopData> gameHotTopListData = new ArrayList<>();
        
        ZoneId zone = ZoneId.of("UTC");
        // get or convert time range to filter out unneeded entry in fact_vendor_game_total
        Long endTimeStampInSeconds;
        Long initialTimeStampInSeconds;
        Vendor vendor = new Vendor();
        if (dto.getVendorCode() != null && !dto.getVendorCode().isEmpty()) {
        	vendor = vendorService.findVendorByCode(dto.getVendorCode());
        }
        // check if its in test environment to allow & get timestamp @ start of the yesterday for QA env testing
        if (isTestEnvironment) {
        	LocalDate endTimeStamp = LocalDate.now(zone);
        	endTimeStampInSeconds = endTimeStamp.atStartOfDay(zone).toEpochSecond();
        	LocalDate initialTimeStamp = LocalDate.now(zone).minusDays(1);
        	initialTimeStampInSeconds = initialTimeStamp.atStartOfDay(zone).toEpochSecond();
        } else {
        	// get timestamp @ start of the month if user intend to get game list from start of the month
            if (dto.getDateRangeType().equals(DateRangeType.MONTHLY.toString().toLowerCase())) {
            	LocalDate initialTimeStamp = LocalDate.now(zone).minusMonths(1).withDayOfMonth(1);
            	initialTimeStampInSeconds = initialTimeStamp.atStartOfDay(zone).toEpochSecond();
            	LocalDate endTimeStamp = LocalDate.now(zone).withDayOfMonth(1);
            	endTimeStampInSeconds = endTimeStamp.atStartOfDay(zone).toEpochSecond();
            // get timestamp @ start of the week (Sunday) if user intend to get game list from start of the week
            } else {
            	LocalDate endTimeStamp = LocalDate.now(zone).minusWeeks(1).with(DayOfWeek.SUNDAY);
            	endTimeStampInSeconds = endTimeStamp.atStartOfDay(zone).toEpochSecond();
            	LocalDate initialTimeStamp = LocalDate.now(zone).minusWeeks(2).with(DayOfWeek.SUNDAY);
            	initialTimeStampInSeconds = initialTimeStamp.atStartOfDay(zone).toEpochSecond();
            }
        }
        
        // generate sql stmt
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SELECT ");
        stringBuilder.append("gamelist.vendorName AS name, ");
        stringBuilder.append("gamelist.vendorCode AS code, ");
        stringBuilder.append("gamelist.gameCode, ");
        stringBuilder.append("gamelist.GameName AS gameName, ");
        stringBuilder.append("gamelist.categoryCode, ");
        stringBuilder.append("IFNULL( concat(:gameUrl, (gamelist.defaultImageSquare)), null) AS imageSquare, ");
		stringBuilder.append("IFNULL( concat(:gameUrl, (gamelist.defaultImageLanscape)), null) AS imageLanscape, ");
		stringBuilder.append("gamelist.languageCode, ");
		stringBuilder.append("gamelist.platformCode, ");
		stringBuilder.append("gamelist.currencyCode ");
		stringBuilder.append("FROM ");
		stringBuilder.append("(SELECT ");
		if (dto.getType().equals(HotTopGameType.TOP.toString().toLowerCase())) {
        	stringBuilder.append("fvgt.total_win_loss AS hotTopWinLoss, ");
        } else {
        	stringBuilder.append("fvgt.total_bet_count AS hotTopBetCount, ");
        }
		stringBuilder.append("vg.id AS gameID, ");
		stringBuilder.append("vg.code AS gameCode, vg.name as GameName, ");
		stringBuilder.append("vg.image_square AS defaultImageSquare, vg.image_landscape AS defaultImageLanscape, ");
		stringBuilder.append("vgc.status, ");
		stringBuilder.append("gc.code AS categoryCode, v.name AS vendorName, v.code AS vendorCode, ");
		stringBuilder.append("GROUP_CONCAT(DISTINCT l.code SEPARATOR ',') AS languageCode, ");
		stringBuilder.append("GROUP_CONCAT(DISTINCT p.code SEPARATOR ',') AS platformCode, ");
		stringBuilder.append("GROUP_CONCAT(DISTINCT c.code SEPARATOR ',') AS currencyCode ");
		stringBuilder.append("FROM vendor_game_codes AS vgc ");
		stringBuilder.append("INNER JOIN vendor_games vg ON vgc.vendor_game_id = vg.id ");
		stringBuilder.append("INNER JOIN fact_vendor_game_total fvgt ON fvgt.vendor_game_id = vg.id ");
		stringBuilder.append("INNER JOIN languages l on l.id = vgc.language_id ");
		stringBuilder.append("INNER JOIN platforms p on p.id = vgc.platform_id ");
		stringBuilder.append("INNER JOIN game_categories gc on vg.game_category_id = gc.id ");
		stringBuilder.append("INNER JOIN vendors v on vg.vendor_id = v.id ");
		stringBuilder.append("INNER JOIN vendor_game_currencies vgcurrency on vg.id = vgcurrency.vendor_game_id ");
		stringBuilder.append("INNER JOIN currencies c on c.id = vgcurrency.currency_id ");
		stringBuilder.append("WHERE vg.id IN ");
		stringBuilder.append("(SELECT hotTopCriteria.selectedVendorGameId FROM ");
		stringBuilder.append("(SELECT fvgt.vendor_game_id AS selectedVendorGameId, ");
		if (dto.getType().equals(HotTopGameType.TOP.toString().toLowerCase())) {
        	stringBuilder.append("SUM(fvgt.total_win_loss) AS totalWinLossSum ");
        } else {
        	stringBuilder.append("SUM(fvgt.total_bet_count) AS totalBetCountSum ");
        }
		stringBuilder.append("FROM fact_vendor_game_total fvgt ");
		stringBuilder.append("WHERE `day` BETWEEN :startTime AND :endTime ");
        stringBuilder.append("AND currency_id = :currencyId ");
        if (dto.getVendorCode() != null && !dto.getVendorCode().isEmpty()) {
        	stringBuilder.append("AND vendor_id = :vendorId ");
        }
        stringBuilder.append("GROUP BY vendor_game_id ");
        if (dto.getType().equals(HotTopGameType.TOP.toString().toLowerCase())) {
        	stringBuilder.append("ORDER BY totalWinLossSum DESC ");
        } else {
        	stringBuilder.append("ORDER BY totalBetCountSum DESC ");
        }
        stringBuilder.append(") ");
        stringBuilder.append("AS hotTopCriteria WHERE ");
        if (isTestEnvironment) {
        	if (dto.getType().equals(HotTopGameType.TOP.toString().toLowerCase())) {
            	stringBuilder.append("hotTopCriteria.totalWinLossSum < -1000 ");
            } else {
            	stringBuilder.append("hotTopCriteria.totalBetCountSum > 100 ");
            }
        } else {
        	if (dto.getType().equals(HotTopGameType.TOP.toString().toLowerCase())) {
            	stringBuilder.append("hotTopCriteria.totalWinLossSum < -100000 ");
            } else {
            	stringBuilder.append("hotTopCriteria.totalBetCountSum > 10000 ");
            }
        }
        stringBuilder.append(") ");
        stringBuilder.append("GROUP BY vg.id ");
        stringBuilder.append("ORDER BY ");
        if (dto.getType().equals(HotTopGameType.TOP.toString().toLowerCase())) {
        	stringBuilder.append("hotTopWinLoss ");
        } else {
        	stringBuilder.append("hotTopBetCount ");
        }
        stringBuilder.append("DESC LIMIT 10) ");
        stringBuilder.append("AS gamelist ");
        
        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("currencyId", currencyId);
        queryParams.put("gameUrl", imageUrl);
        queryParams.put("startTime", initialTimeStampInSeconds);
        queryParams.put("endTime", endTimeStampInSeconds);
        if (dto.getVendorCode() != null && !dto.getVendorCode().isEmpty()) {
        	queryParams.put("vendorId", vendor.getId());
        }
        
        TypedQuery<Tuple> query = (TypedQuery<Tuple>) entityManager.createNativeQuery(stringBuilder.toString(), Tuple.class);
        //Execute query
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        List<Tuple> resultList = query.getResultList();
        
        // only extract needed information and return to VO
        if (!resultList.isEmpty()) {
        	for (Tuple result : resultList) {
        		
        		GameHotTopData gameHotTopData = new GameHotTopData();
        		gameHotTopData.setName(result.get("name") != null ? result.get("name").toString() : null);
        		gameHotTopData.setCode(result.get("code") != null ? result.get("code").toString() : null);
            	gameHotTopData.setGameName(result.get("gameName") != null ? result.get("gameName").toString() : null);
            	gameHotTopData.setCurrencyCode(result.get("currencyCode") != null ? result.get("currencyCode").toString() : null);
            	gameHotTopData.setGameCode(result.get("gameCode") != null ? result.get("gameCode").toString() : null);
            	gameHotTopData.setCategoryCode(result.get("categoryCode") != null ? result.get("categoryCode").toString() : null);
            	gameHotTopData.setImageSquare(result.get("imageSquare") != null ? result.get("imageSquare").toString() : null);
            	gameHotTopData.setImageLandscape(result.get("imageLanscape") != null ? result.get("imageLanscape").toString() : null);
            	gameHotTopData.setLanguageCode(result.get("languageCode") != null ? result.get("languageCode").toString() : null);
            	gameHotTopData.setPlatformCode(result.get("platformCode") != null ? result.get("platformCode").toString() : null);
            	gameHotTopListData.add(gameHotTopData);
        	}
        }
        
        return gameHotTopListData;
    }

}