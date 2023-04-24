package com.nextgen.gameaggregator.operator.transactions.list;

import com.nextgen.gameaggregator.repository.BetHistoryRepository;
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
public class TransactionListService {

    @Autowired
    private BetHistoryRepository betHistoryRepository;

    public TransactionsListData getTransactionsList(TransactionsListDto dto, Integer agentId){
        //TODO (by Alex), to discuss and change fetch by updated time

        List<Sort.Order> orders = this.generateOrder();
        Pageable pagingSort = PageRequest.of(dto.getPageNo() - 1, dto.getPageSize(), Sort.by(orders));

        Page<Object> transactionsList =  betHistoryRepository.findByAgentIdAndCreateTimeBetween(
                agentId, dto.getFromTime(), dto.getToTime(), pagingSort);

        TransactionsListData transactionsListData = new TransactionsListData();
        transactionsListData.setHeaders(this.getHeaders());

        transactionsListData.setTransactions(transactionsList.getContent());
        transactionsListData.setCurrentPage(transactionsList.getNumber() + 1);
        transactionsListData.setTotalItems(transactionsList.getTotalElements());
        transactionsListData.setTotalPages(transactionsList.getTotalPages());

        return transactionsListData;

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

    //region generate headers
    private HashMap<String, Integer> getHeaders() {
        HashMap<String, Integer> hm = (new HashMap<String, Integer>() {{
            put("transactionId", 0);
            put("externalTransactionId", 1);
            put("roundId", 2);
            put("username", 3);
            put("currencyCode", 4);
            put("gameCode", 5);
            put("vendorCode", 6);
            put("gameCategoryCode", 7);
            put("betAmount", 8);
            put("winAmount", 9);
            put("winLoss", 10);
            put("effectiveTurnover", 11);
            put("jackpotAmount", 12);
            put("refundAmount", 13);
            put("status", 14);
            put("vendorBetTime", 15);
            put("vendorSettleTime", 16);
            put("isFreeSpin", 17);
        }});

        return sortByValue(hm);
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
