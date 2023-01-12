package com.nextgen.gameaggregator.operator.transactions.list;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class TransactionsListData {
    private HashMap<String, Integer> headers;

    private List<List<Object>> transactions;

    private Integer currentPage;

    private Long totalItems;

    private Integer totalPages;
}
