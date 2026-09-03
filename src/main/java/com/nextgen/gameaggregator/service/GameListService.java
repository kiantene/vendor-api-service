package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.AgentVendorLine;
import com.nextgen.gameaggregator.entity.ga.Language;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.operator.game.list.GameListData;
import com.nextgen.gameaggregator.operator.game.list.GameListDto;
import com.nextgen.gameaggregator.repository.ga.reader.VendorGameReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class GameListService {

    @Autowired
    private VendorGameReaderRepository vendorGameReaderRepository;

    @Value("${image.gameurl}")
    private String imageUrl;



    public GameListData getGameList(GameListDto dto, List<AgentVendorLine> agentVendorLines, Vendor vendor, List<Integer> currencyIds, Language language,
    Agent agent
    ) {
        GameListData gameListData = new GameListData();

        List<Integer> gameCategoryIds = new ArrayList<>();
        for (AgentVendorLine agentVendorLine : agentVendorLines) {
            gameCategoryIds.add(agentVendorLine.getGameCategoryId());
        }

        // ONEAPI-529: the page is taken inside the query and assembled here. Handing a Pageable
        // to Spring Data made it wrap the statement in a second page clause and run its count on
        // every call whose first page came back full -- and that count was the same heavy shape
        // as the list. Both statements now share one filter, so the total and the pages agree.
        int pageSize = dto.getPageSize();
        int offset = (dto.getPageNo() - 1) * pageSize;

        List<Object> games = vendorGameReaderRepository.findByVendorIdAndStatusAndLanguageAndCategoryAndCurrency
                (vendor.getId(), Status.ACTIVE.code, gameCategoryIds, currencyIds, language.getId(), imageUrl,
                        agent.getHouseId(), agent.getMasterAgentId(), agent.getId(), pageSize, offset);

        long totalItems = vendorGameReaderRepository.countByVendorIdAndStatusAndCategoryAndCurrency
                (vendor.getId(), Status.ACTIVE.code, gameCategoryIds, currencyIds,
                        agent.getHouseId(), agent.getMasterAgentId(), agent.getId());

        gameListData.setHeaders(this.getHeaders());
        gameListData.setGames(games);
        gameListData.setCurrentPage(dto.getPageNo());
        gameListData.setTotalItems(totalItems);
        gameListData.setTotalPages((int) Math.ceil((double) totalItems / pageSize));
        return gameListData;
    }

    private HashMap<String, Integer> getHeaders() {
        HashMap<String, Integer> hm = (new HashMap<String, Integer>() {{
            put("gameCode", 0);
            put("gameName", 1);
            put("categoryCode", 2);
            put("imageSquare", 3);
            put("imageLandscape", 4);
            put("languageCode", 5);
            put("platformCode", 6);
            put("currencyCode", 7);
        }});

        return sortByValue(hm);
    }

    public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer>> list =
                new LinkedList<Map.Entry<String, Integer>>(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }
}
