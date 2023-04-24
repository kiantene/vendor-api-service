package com.nextgen.gameaggregator.vendor.jdb.constant;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class GameCategory {
    public static final Multimap<Integer, String> CATEGORY = ArrayListMultimap.create();
    
    static {
        CATEGORY.put(1, "0");
        CATEGORY.put(1, "8003");
        CATEGORY.put(1, "66");
        CATEGORY.put(1, "66001");
        CATEGORY.put(1, "26");
        CATEGORY.put(1, "26003");
        CATEGORY.put(1, "9");
        CATEGORY.put(1, "9001");
        CATEGORY.put(1, "22");
        CATEGORY.put(1, "22001");
        CATEGORY.put(2, "7");
        CATEGORY.put(2, "7001");
        CATEGORY.put(2, "67");
        CATEGORY.put(2, "67001");
        CATEGORY.put(2, "27");
        CATEGORY.put(2, "27017");
        CATEGORY.put(4, "18");
        CATEGORY.put(4, "18026");
    }
}
