package com.nextgen.gameaggregator.operator.game.list;

import com.nextgen.gameaggregator.entity.Vendor;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.repository.VendorGameRepository;
import com.nextgen.gameaggregator.repository.VendorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private VendorRepository vendorRepository;

    public GameListData getGameList(GameListDto dto) throws RecordNotFoundException {
        GameListData gameListData = new GameListData();

        Vendor vendor = vendorRepository.findByCode(dto.getVendorCode());

        Optional.ofNullable(vendor).orElseThrow(RecordNotFoundException::new);

        List<Sort.Order> orders = this.generateOrder();
        Pageable pagingSort = PageRequest.of(dto.getPageNo() - 1, dto.getSize(), Sort.by(orders));

        Page<Object> gameList =  vendorGameRepository.findByVendorIdAndStatus(vendor.getId(), Status.ACTIVE.code, pagingSort);

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
            put("categoryCode", 1);
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

    public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer> > list =
                new LinkedList<Map.Entry<String, Integer> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2)
            {
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
