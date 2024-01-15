package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentVendorLine;
import com.nextgen.gameaggregator.entity.ga.Language;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.operator.game.list.GameListData;
import com.nextgen.gameaggregator.operator.game.list.GameListDto;
import com.nextgen.gameaggregator.repository.ga.writer.LanguageRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorGameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class GameListService {

    @Autowired
    private VendorGameRepository vendorGameRepository;
    @Autowired
    private LanguageRepository languageRepository;

    @Value("${image.gameurl}")
    private String imageUrl;



    public GameListData getGameList(GameListDto dto, List<AgentVendorLine> agentVendorLines, Vendor vendor, List<Integer> currencyIds, Language language) {
        GameListData gameListData = new GameListData();

        List<Integer> gameCategoryIds = new ArrayList<>();
        for (AgentVendorLine agentVendorLine : agentVendorLines) {
            gameCategoryIds.add(agentVendorLine.getGameCategory().getId());
        }

        List<Sort.Order> orders = this.generateOrder();
        Pageable pagingSort = PageRequest.of(dto.getPageNo() - 1, dto.getPageSize(), Sort.by(orders));

        Page<Object> gameList = vendorGameRepository.findByVendorIdAndStatusAndLanguageAndCategoryAndCurrency
                (vendor.getId(), Status.ACTIVE.code, gameCategoryIds, currencyIds, language.getId(), imageUrl, pagingSort);

        gameListData.setHeaders(this.getHeaders());
        gameListData.setGames(gameList.getContent());
        gameListData.setCurrentPage(gameList.getNumber() + 1);
        gameListData.setTotalItems(gameList.getTotalElements());
        gameListData.setTotalPages(gameList.getTotalPages());
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

    //TODO add order by param
    private List<Sort.Order> generateOrder() {
        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        //region TODO sorting
//            if (sort[0].contains(",")) {
//                // will sort more than 2 fields
//                // sortOrder="field, direction"
//                for (String sortOrder : sort) {
//                    String[] _sort = sortOrder.split(",");
//                    orders.add(new Order(getSortDirection(_sort[1]), _sort[0]));
//                }
//            } else {
//                // sort=[field, direction]
//                orders.add(new Order(getSortDirection(sort[1]), sort[0]));
//            }
        //endregion
        return orders;
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
