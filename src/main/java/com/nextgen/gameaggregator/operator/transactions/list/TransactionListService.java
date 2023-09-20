package com.nextgen.gameaggregator.operator.transactions.list;

import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.repository.BetHistoryRepository;
import com.nextgen.gameaggregator.util.MysqlUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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
    @Autowired
    private MysqlUtils mysqlUtils;

    @PersistenceContext
    private EntityManager entityManager;

    public TransactionsListData getTransactionsList(TransactionsListDto dto, Integer agentId){
        //TODO (by Alex), to discuss and change fetch by updated time

        List<Sort.Order> orders = this.generateOrder();
        TransactionsListData transactionsListData = this.findByAgentIdAndCreateTimeBetween(agentId, dto);
        transactionsListData.setHeaders(this.getHeaders());

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
            put("betId", 0);
            put("roundId", 1);
            put("externalTransactionId", 2);
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
            put("status", 13);
            put("vendorBetTime", 14);
            put("vendorSettleTime", 15);
            put("isFreeSpin", 16);
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

    private TransactionsListData findByAgentIdAndCreateTimeBetween(Integer agentId, TransactionsListDto dto) {
        //form partitions
        long fromTime = dto.getFromTime();
        long toTime = dto.getToTime();
        String partitionsString = mysqlUtils.convertDateTimeToDayPartitions(fromTime, toTime,"bet_history");
        String sqlStmt = "SELECT " +
            "bh.id AS betId, " +
            "bh.round_id AS roundId, " +
            "bh.vendor_bet_id AS externalTransactionId, " +
            "ap.username AS username, " +
            "c.code AS currencyCode, " +
            "vg.code AS gameCode, " +
            "v.code AS vendorCode, " +
            "gc.code AS gameCategoryCode, " +
            "bh.bet_amount AS betAmount, " +
            "bh.win_amount AS winAmount, " +
            "bh.win_loss AS winLoss, " +
            "bh.effective_turnover AS effectiveTurnover, " +
            "bh.jackpot_amount AS jackpotAmount, " +
            "bh.status AS status, " +
            "bh.vendor_bet_time AS vendorBetTime, " +
            "bh.vendor_settle_time AS vendorSettleTime, " +
            "IF(bh.is_freespin =0 ,'FALSE','TRUE') AS isFreeSpin "+
            "FROM bet_history "+partitionsString+" AS bh " +
            "INNER JOIN agent_players AS ap ON ap.id = bh.agent_player_id " +
            "INNER JOIN vendor_players AS vp ON vp.id = bh.vendor_player_id " +
            "INNER JOIN currencies AS c ON c.id = bh.currency_id " +
            "INNER JOIN vendor_games AS vg ON vg.id = bh.vendor_game_id " +
            "INNER JOIN vendors AS v ON v.id = bh.vendor_id " +
            "INNER JOIN game_categories AS gc ON gc.id = vg.game_category_id " +
            " WHERE bh.agent_id =:agentId AND bh.vendor_bet_time BETWEEN :fromTime AND :toTime ORDER BY bh.vendor_bet_time DESC ";


        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("agentId", agentId);
        queryParams.put("fromTime", fromTime);
        queryParams.put("toTime", toTime);

        Query query = entityManager.createNativeQuery(sqlStmt);
        //Execute query
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        int currentPage = dto.getPageNo();
        int offset = (currentPage - 1) * dto.getPageSize();
        int limit = dto.getPageSize();

        TransactionsListData transData = new TransactionsListData();
        transData.setCurrentPage(currentPage);
        transData.setTotalItems((long) query.getResultList().size());
        transData.setTotalPages((int) Math.ceil((double) query.getResultList().size() / limit));
        query.setFirstResult(offset).setMaxResults(limit);
        transData.setTransactions(query.getResultList());

        return transData;
    }

}
