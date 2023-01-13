package com.nextgen.gameaggregator.controller;

import lombok.Data;

import java.util.List;

@Data
public class PgGameListResponseVo {
    List<PgGameResponse> data;
}
