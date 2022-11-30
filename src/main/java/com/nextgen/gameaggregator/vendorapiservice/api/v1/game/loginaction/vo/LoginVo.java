package com.nextgen.gameaggregator.vendorapiservice.api.v1.game.loginaction.vo;

public class LoginVo {

    private String response1;
    private String response2;

    public LoginVo(String response1, String response2) {
        this.response1 = response1;
        this.response2 = response2;
    }

    public String getResponse1() {
        return response1;
    }

    public void setResponse1(String response1) {
        this.response1 = response1;
    }

    public String getResponse2() {
        return response2;
    }

    public void setResponse2(String response2) {
        this.response2 = response2;
    }
}
