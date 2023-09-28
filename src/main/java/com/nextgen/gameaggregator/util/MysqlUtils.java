package com.nextgen.gameaggregator.util;

import com.nextgen.sas.core.util.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class MysqlUtils {
    @PersistenceContext
    private EntityManager entityManager;

    public  String convertDateTimeToDayPartitions(Long dateStartMilli, Long dateEndMilli, String tableName) {
        //note: Convert all the time to UTC for partition because partition in the DB is UTC
        String partitionsString = "";
        String dateFormat = "yyyy-MM-dd";
        List<String> partitions = new ArrayList<>();
        String startDateStr = DateUtil.convertMiliToDateString(dateStartMilli, "UTC", dateFormat);
        String endDateStr = DateUtil.convertMiliToDateString(dateEndMilli, "UTC", dateFormat);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        LocalDate begin = LocalDate.parse(startDateStr, formatter);
        LocalDate end = LocalDate.parse(endDateStr, formatter);
        log.error("begin time : " + begin);
        log.error("end time : " + end);

        for (LocalDate i = begin; !i.isAfter(end); i = i.plusDays(1)) {
            partitions.add("P_" + i.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        }

        if(!partitions.isEmpty()) {
            //get existing partitions from table, use partitions that exist in table only to prevent errors
            List<String> partitionsFilter = getPartitionByTableName(tableName);
            partitions.retainAll(partitionsFilter); //get intersection partition
        }
        //check again after intersection
        if(!partitions.isEmpty()) {
            partitionsString = " PARTITION (" + String.join(",", partitions) + ") ";
            System.out.println("PARTITIONS -> " + partitionsString);
        }
        return partitionsString;
    }
    public List<String> getPartitionByTableName(String tableName) {
        String queryStr = "SELECT PARTITION_NAME FROM INFORMATION_SCHEMA.PARTITIONS WHERE TABLE_NAME = :tableName AND PARTITION_NAME IS NOT NULL";
        List<String> partitions = new ArrayList<>();
        Query query = entityManager.createNativeQuery(queryStr);
        query.setParameter("tableName", tableName);
        //Execute query
        // Execute query and map results
        List<Object> result = query.getResultList();
        if(result != null && !result.isEmpty()) {
            for (Object row : result) {
                partitions.add((String) row);
            }
        }

        return partitions;
    }
    public List<String> convertDateTimeToMonthPartitions(Long dateStartMilli, Long dateEndMilli, String tableName) {
        // note: Convert all the time to UTC for partition because partition in the DB is UTC
        String dateFormat = "yyyy-MM-dd";
        List<String> partitions = new ArrayList<>();
        String startDateStr =  DateUtil.convertMiliToDateString(dateStartMilli, "UTC", dateFormat);
        String endDateStr = DateUtil.convertMiliToDateString(dateEndMilli, "UTC", dateFormat);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        LocalDate begin = LocalDate.parse(startDateStr, formatter);
        LocalDate end = LocalDate.parse(endDateStr, formatter);

        LocalDate currentDate = begin;
        while (!currentDate.isAfter(end.plusMonths(1))) {
            String formattedMonth = currentDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
            partitions.add("P_" +formattedMonth); //form month partitions
            currentDate = currentDate.plusMonths(1);
        }
        if(!partitions.isEmpty()) {
            List<String> partitionsFilter = getPartitionByTableName(tableName);
            partitions.retainAll(partitionsFilter); //get intersection partition

        }

        log.info("partitionsFilter ---> begin : " + String.join(",", partitions) );
        return partitions;
    }


}
