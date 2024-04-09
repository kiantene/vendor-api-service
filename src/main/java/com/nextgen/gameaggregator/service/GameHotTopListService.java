package com.nextgen.gameaggregator.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.ga.AgentVendorLine;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.enums.DateRangeType;
import com.nextgen.gameaggregator.enums.HotTopGameType;
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

    public List<GameHotTopData> getHotTopGameList(GameHotTopListDto dto, List<AgentVendorLine> agentVendorLines, Vendor vendor, List<Integer> currencyIds) {
        List<GameHotTopData> gameHotTopListData = new ArrayList<>();

        List<Integer> gameCategoryIds = new ArrayList<>();
        for (AgentVendorLine agentVendorLine : agentVendorLines) {
            gameCategoryIds.add(agentVendorLine.getGameCategory().getId());
        }
        
        ZoneId zone = ZoneId.of("UTC");
        Long currentTimeStamp = System.currentTimeMillis() / 1000;
        Long targetTimeStampInSeconds;
        if (dto.getDateRangeType().equals(DateRangeType.MONTHLY.toString().toLowerCase())) {
        	LocalDate targetTimeStamp = LocalDate.now(zone).withDayOfMonth(1);
        	targetTimeStampInSeconds = targetTimeStamp.atStartOfDay(zone).toEpochSecond();
        } else {
        	LocalDate targetTimeStamp = LocalDate.now(zone).with(DayOfWeek.SUNDAY);
        	targetTimeStampInSeconds = targetTimeStamp.atStartOfDay(zone).toEpochSecond() - (7 * (DateUtils.MILLIS_PER_DAY / 1000));
        }
        
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SELECT ");
        stringBuilder.append("gamelist.gameCode, ");
        stringBuilder.append("IFNULL(languageList.langName, gamelist.GameName) AS name, ");
        stringBuilder.append("gamelist.categoryCode, ");
        stringBuilder.append("IFNULL( concat(:gameUrl, (IFNULL(languageList.langImageSquare, gamelist.defaultImageSquare))), null) AS imageSquare, ");
		stringBuilder.append("IFNULL( concat( :gameUrl, (IFNULL(languageList.langImageLandscape, gamelist.defaultImageLanscape))), null) AS imageLanscape, ");
		stringBuilder.append("gamelist.languageCode, ");
		stringBuilder.append("gamelist.platformCode, ");
		stringBuilder.append("gamelist.currencyCode ");
		stringBuilder.append("FROM ");
		stringBuilder.append("(SELECT ");
		stringBuilder.append("vg.id AS gameID, ");
		stringBuilder.append("vg.code AS gameCode, vg.name as GameName, ");
		stringBuilder.append("vg.image_square AS defaultImageSquare, vg.image_landscape AS defaultImageLanscape, ");
		stringBuilder.append("vgc.status, ");
		stringBuilder.append("gc.code AS categoryCode, v.code AS vendorCode, ");
		stringBuilder.append("GROUP_CONCAT(DISTINCT l.code SEPARATOR ',') AS languageCode, ");
		stringBuilder.append("GROUP_CONCAT(DISTINCT p.code SEPARATOR ',') AS platformCode, ");
		stringBuilder.append("GROUP_CONCAT(DISTINCT c.code SEPARATOR ',') AS currencyCode ");
		stringBuilder.append("FROM vendor_game_codes AS vgc ");
		stringBuilder.append("INNER JOIN vendor_games vg ON vgc.vendor_game_id = vg.id ");
		stringBuilder.append("INNER JOIN languages l on l.id = vgc.language_id  ");
		stringBuilder.append("INNER JOIN platforms p on p.id = vgc.platform_id ");
		stringBuilder.append("INNER JOIN game_categories gc on vg.game_category_id = gc.id ");
		stringBuilder.append("INNER JOIN vendors v on vg.vendor_id = v.id ");
		stringBuilder.append("INNER JOIN vendor_game_currencies vgcurrency on vg.id = vgcurrency.vendor_game_id ");
		stringBuilder.append("INNER JOIN currencies c on c.id = vgcurrency.currency_id ");
		stringBuilder.append("WHERE vgc.vendor_game_id IN ");
		stringBuilder.append("(SELECT fvgt.vendor_game_id FROM fact_vendor_game_total fvgt WHERE `day` BETWEEN :startTime AND :endTime ORDER BY ");
        if (dto.getType().equals(HotTopGameType.TOP.toString())) {
        	stringBuilder.append("ggr ");
        } else {
        	stringBuilder.append("total_bet_count ");
        }
        stringBuilder.append("DESC) ");
        stringBuilder.append("AND vgcurrency.currency_id IN (:currencyIds) ");
        stringBuilder.append("GROUP BY  vg.id ");
        stringBuilder.append("ORDER BY vg.code, l.code,  p.code LIMIT 10) AS gamelist ");
        stringBuilder.append("LEFT JOIN ( SELECT vgcl.name as langName, vgcl.image_square as langImageSquare, vgcl.image_landscape as langImageLandscape, vgcl.vendor_game_id ");
        stringBuilder.append("FROM vendor_game_codes vgcl  ");
        stringBuilder.append("WHERE vgcl.vendor_id =:vendorId GROUP BY vgcl.vendor_game_id, vgcl.name ");
        stringBuilder.append(") AS languageList ");
        stringBuilder.append("ON languageList.vendor_game_id = gamelist.gameID");
        
        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("vendorId", vendor.getId());
        queryParams.put("currencyIds", currencyIds);
        queryParams.put("gameUrl", imageUrl);
        queryParams.put("startTime", targetTimeStampInSeconds);
        queryParams.put("endTime", currentTimeStamp);
        TypedQuery<Tuple> query = (TypedQuery<Tuple>) entityManager.createNativeQuery(stringBuilder.toString(), Tuple.class);
        //Execute query
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        List<Tuple> resultList = query.getResultList();
        
        if (!resultList.isEmpty()) {
        	for (Tuple result : resultList) {
        		
        		GameHotTopData gameHotTopData = new GameHotTopData();
            	gameHotTopData.setName(result.get("name") != null ? result.get("name").toString() : null);
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