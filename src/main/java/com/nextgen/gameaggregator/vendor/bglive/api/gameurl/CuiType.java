package com.nextgen.gameaggregator.vendor.bglive.api.gameurl;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CuiType {
    DISABLE_DOWNLOAD_BUTTON_H5("cui", "16");

    private final String key;
    private final String value;
}
