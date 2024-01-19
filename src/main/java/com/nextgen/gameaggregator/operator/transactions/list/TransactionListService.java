package com.nextgen.gameaggregator.operator.transactions.list;

import com.nextgen.gameaggregator.exception.InvalidDateRangeException;
import com.nextgen.gameaggregator.repository.ga.writer.BetHistoryRepository;
import com.nextgen.gameaggregator.util.MysqlUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.nextgen.gameaggregator.exception.InvalidFromTimeException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Transactional(transactionManager = "transactionManagerGaServiceReaderDb", isolation = Isolation.READ_UNCOMMITTED) //allow dirty read to prevent table locking
    public TransactionsListData findByAgentIdAndCreateTimeBetween(Integer agentId, TransactionsListDto dto) {
        //form partitions
        long fromTime = dto.getFromTime();
        long toTime = dto.getToTime();
        int currentPage = dto.getPageNo();
        int offset = (currentPage - 1) * dto.getPageSize();
        int limit = dto.getPageSize();
        String partitionsString = mysqlUtils.convertDateTimeToDayPartitions(fromTime, toTime,"bet_history");


        String sqlStmt = " SELECT " +
         " bh.id AS betId, bh.round_id AS roundId, bh.vendor_bet_id AS externalTransactionId, ap.username AS username, c.code AS currencyCode, vg.code AS gameCode, v.code AS vendorCode, gc.code AS gameCategoryCode, bh.bet_amount AS betAmount, bh.win_amount AS winAmount, bh.win_loss AS winLoss, bh.effective_turnover AS effectiveTurnover, bh.jackpot_amount AS jackpotAmount, bh.status AS status, bh.vendor_bet_time AS vendorBetTime, bh.vendor_settle_time AS vendorSettleTime, IF(bh.is_freespin =0 ,'FALSE','TRUE') AS isFreeSpin " +
         " FROM " +
        " (SELECT id , round_id , vendor_bet_id, agent_player_id, vendor_player_id, currency_id, vendor_game_id, vendor_id, game_category_id, bet_amount, win_amount, win_loss, effective_turnover, jackpot_amount, status, vendor_bet_time , vendor_settle_time , is_freespin "+
        " FROM bet_history " + partitionsString +
        " WHERE agent_id = :agentId AND vendor_bet_time BETWEEN :fromTime AND :toTime "+
        " ORDER BY agent_id DESC , vendor_bet_time DESC limit :offset,:limit ) AS bh "+
        " INNER JOIN agent_players AS ap ON ap.id = bh.agent_player_id " +
        " INNER JOIN vendor_players AS vp ON vp.id  = bh.vendor_player_id "+
        " INNER JOIN currencies AS c ON c.id = bh.currency_id " +
        " INNER JOIN vendor_games AS vg ON vg.id  = bh.vendor_game_id " +
        " INNER JOIN vendors AS v ON v.id = bh.vendor_id " +
        " INNER JOIN game_categories AS gc ON gc.id = vg.game_category_id " +
        " ORDER BY vendor_bet_time DESC ";


        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("agentId", agentId);
        queryParams.put("fromTime", fromTime);
        queryParams.put("toTime", toTime);
        queryParams.put("offset", offset);
        queryParams.put("limit", limit);
        Query query = entityManager.createNativeQuery(sqlStmt);
        //Execute query
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        Long resultCount = this.getTotalCount(agentId, dto, partitionsString);
        System.err.println("resultCount : " + resultCount);

        TransactionsListData transData = new TransactionsListData();
        transData.setCurrentPage(currentPage);
        transData.setTotalItems(resultCount);
        transData.setTotalPages((int) Math.ceil((double) resultCount / limit));
        transData.setTransactions(query.getResultList());

        return transData;
    }

    public void isStartTimeValid(long startTimeMillis) throws InvalidFromTimeException {
        // Calculate the current date minus 60 days
        LocalDate currentDateMinus60Days = LocalDate.now().minusDays(60);
        System.out.println("currentDateMinus60Days: "+ currentDateMinus60Days);
        // Convert the LocalDate to LocalDateTime for comparison
        LocalDateTime validStartTime = currentDateMinus60Days.atStartOfDay();
        // Convert the start time in milliseconds to LocalDateTime
        LocalDateTime inputStartTime = LocalDateTime.ofInstant(
            new Date(startTimeMillis).toInstant(),
            ZoneId.systemDefault()
        );
        // Check if the input start time is not before the valid start time
        if(inputStartTime.isBefore(validStartTime)) {
            throw new InvalidFromTimeException();
        }

    }

    private Long getTotalCount(Integer agentId, TransactionsListDto dto, String partitionStr) {
        //form partitions
        long fromTime = dto.getFromTime();
        long toTime = dto.getToTime();

        String sqlStmt = "SELECT COUNT(1) "+ " FROM bet_history " + partitionStr + " WHERE agent_id = :agentId AND vendor_bet_time BETWEEN :fromTime AND :toTime";


        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("agentId", agentId);
        queryParams.put("fromTime", fromTime);
        queryParams.put("toTime", toTime);
        Query query = entityManager.createNativeQuery(sqlStmt);
        //Execute query
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        return Long.parseLong(query.getSingleResult().toString());

    }

    public void isDateRangeValid(long fromTime, long toTime) throws InvalidDateRangeException {
        // Calculate the difference in milliseconds between start and end times
        long differenceInMillis = toTime - fromTime;
        // Define the maximum allowed difference for 1 day (24 hours * 60 minutes * 60 seconds * 1000 milliseconds)
        long maxDifferenceInMillis = 24L * 60L * 60L * 1000L;
        // Compare the difference with the maximum allowed difference
        if(differenceInMillis > maxDifferenceInMillis) {
            throw new InvalidDateRangeException();
        }
    }

}
