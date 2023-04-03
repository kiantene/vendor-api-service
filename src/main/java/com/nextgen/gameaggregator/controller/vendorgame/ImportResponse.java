package com.nextgen.gameaggregator.controller.vendorgame;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ImportResponse {

    private Map<String, String> result = new HashMap<>();

    private Integer totalSuccess = 0;

    private Integer totalFail = 0;

    private String message;
}
