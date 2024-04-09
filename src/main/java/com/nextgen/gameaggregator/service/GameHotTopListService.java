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
import com.nextgen.gameaggregator.operator.game.hottoplist.GameHotTopListData;
import com.nextgen.gameaggregator.operator.game.hottoplist.GameHotTopListDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameHotTopListService {

    @PersistenceContext(unitName = "mysqlPersistenceUnitGaReader")
    private EntityManager entityManager;
    @Value("${image.gameurl}")
    private String imageUrl;

    public List<GameHotTopListData> getHotTopGameList(GameHotTopListDto dto, List<AgentVendorLine> agentVendorLines, Vendor vendor, List<Integer> currencyIds) {
        List<GameHotTopListData> gameHotTopListData = new ArrayList<>();

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
        
        String sql = "SELECT " +
        "gamelist.gameCode, " +
        "IFNULL(languageList.langName, gamelist.GameName) AS name, " +
        "gamelist.categoryCode, " +
        "IFNULL( concat(:gameUrl, (IFNULL(languageList.langImageSquare, gamelist.defaultImageSquare))), null) AS imageSquare, " +
        "IFNULL( concat( :gameUrl, (IFNULL(languageList.langImageLandscape, gamelist.defaultImageLanscape))), null) AS imageLanscape, " +
        "gamelist.languageCode, " +
        "gamelist.platformCode, " +
        "gamelist.currencyCode " +
        "FROM " +
        "(SELECT " +
        "vg.id AS gameID, " +
        "vg.code AS gameCode, vg.name as GameName, " +
        "vg.image_square AS defaultImageSquare, vg.image_landscape AS defaultImageLanscape, " +
        "vgc.status, " +
        "gc.code AS categoryCode, v.code AS vendorCode, " +
        "GROUP_CONCAT(DISTINCT l.code SEPARATOR ',') AS languageCode, " +
        "GROUP_CONCAT(DISTINCT p.code SEPARATOR ',') AS platformCode, " +
        "GROUP_CONCAT(DISTINCT c.code SEPARATOR ',') AS currencyCode " +
        "FROM vendor_game_codes AS vgc " +
        "INNER JOIN vendor_games vg ON vgc.vendor_game_id = vg.id " +
        "INNER JOIN languages l on l.id = vgc.language_id  " +
        "INNER JOIN platforms p on p.id = vgc.platform_id " +
        "INNER JOIN game_categories gc on vg.game_category_id = gc.id " +
        "INNER JOIN vendors v on vg.vendor_id = v.id " +
        "INNER JOIN vendor_game_currencies vgcurrency on vg.id = vgcurrency.vendor_game_id " +
        "INNER JOIN currencies c on c.id = vgcurrency.currency_id " +
        "WHERE vgc.vendor_game_id IN " +
        "(SELECT fvgt.vendor_game_id FROM fact_vendor_game_total fvgt WHERE `day` BETWEEN :startTime AND :endTime ORDER BY ggr DESC) " +
//        dto.getType() == (HotTopGameType.TOP.toString()) ? "ggr ": "total_bet_count " +
//        "DESC) " +
        "AND vgcurrency.currency_id IN (:currencyIds) " +
        "GROUP BY  vg.id " +
        "ORDER BY vg.code, l.code,  p.code LIMIT 10) AS gamelist " +
        "LEFT JOIN ( SELECT vgcl.name as langName, vgcl.image_square as langImageSquare, vgcl.image_landscape as langImageLandscape, vgcl.vendor_game_id " +
        "FROM vendor_game_codes vgcl  " +
        "WHERE vgcl.vendor_id =:vendorId GROUP BY vgcl.vendor_game_id, vgcl.name " +
        ") AS languageList " +
        "ON languageList.vendor_game_id = gamelist.gameID";
        
        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("vendorId", vendor.getId());
        queryParams.put("currencyIds", currencyIds);
        queryParams.put("gameUrl", imageUrl);
        queryParams.put("startTime", targetTimeStampInSeconds);
        queryParams.put("endTime", currentTimeStamp);
        Query query = entityManager.createNativeQuery(sql);
        //Execute query
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
               
        gameHotTopListData = query.getResultList();
        
        return gameHotTopListData;
    }

}