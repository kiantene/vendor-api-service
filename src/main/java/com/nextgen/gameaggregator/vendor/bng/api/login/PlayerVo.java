package com.nextgen.gameaggregator.vendor.bng.api.login;

import lombok.Data;

@Data
public class PlayerVo {
    private String id;
    private String brand;
    private String currency;
    private String mode;
    private boolean is_test;

    public boolean isIs_test() {
        return is_test;
    }

    public void setIs_test(boolean is_test) {
        this.is_test = is_test;
    }
}
