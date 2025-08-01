package com.nextgen.gameaggregator.repository.ga.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class BetHistoryRepository {

    private final NamedParameterJdbcTemplate clickHouseJdbcTemplate;

    public BetHistoryRepository(@Qualifier("clickHouseJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.clickHouseJdbcTemplate = jdbcTemplate;
    }

    public List<BetHistory> getBetHistoryByVendorSettleTimeAndExternalId(
            String externalTransactionId,
            long toTimeMillis) throws IncorrectResultSizeDataAccessException, DataAccessException {

        long sixHoursInMillis = 21600000L;
        // calculate fromTime
        long fromTimeMillis = toTimeMillis - sixHoursInMillis;

        String sql = "SELECT * FROM bet_history " +
                "WHERE vendor_settle_time BETWEEN :fromTime AND :toTime " +
                "AND external_transaction_id = :externalTransactionId";

        Map<String, Object> params = Map.of(
                "fromTime", fromTimeMillis,
                "toTime", toTimeMillis,
                "externalTransactionId", externalTransactionId
        );

        // 印出所有參數
        params.forEach((key, value) -> System.out.println(key + " = " + value));

        return clickHouseJdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(BetHistory.class))
                .isEmpty() ? null : clickHouseJdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(BetHistory.class));

    }


}
