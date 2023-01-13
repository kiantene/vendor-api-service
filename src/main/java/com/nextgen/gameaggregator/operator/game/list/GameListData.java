package com.nextgen.gameaggregator.operator.game.list;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
@Data
public class GameListData {
    private HashMap<String, Integer> headers;

    private List<Object> games;

    private Integer currentPage;

    private Long totalItems;

    private Integer totalPages;
}
