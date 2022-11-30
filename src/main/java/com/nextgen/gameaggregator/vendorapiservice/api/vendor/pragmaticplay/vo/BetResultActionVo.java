package com.nextgen.gameaggregator.vendorapiservice.api.vendor.pragmaticplay.vo;

public class BetResultActionVo extends AbstractActionVo {
    private String transactionId;
    private String currency;
    private String cash;
    private String bonus;

    public BetResultActionVo() {
        super();
    }

    public BetResultActionVo(String transactionId, String currency, String cash, String bonus, String usedPromo, String error, String description) {
        super();
        this.transactionId = transactionId;
        this.currency = currency;
        this.cash = cash;
        this.bonus = bonus;
        super.setStatus(true);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCash() {
        return cash;
    }

    public void setCash(String cash) {
        this.cash = cash;
    }

    public String getBonus() {
        return bonus;
    }

    public void setBonus(String bonus) {
        this.bonus = bonus;
    }

    public String getDescription() {
        return this.getError().getMessage();
    }

    public String getErrorCode() {
        return this.getError().getCode();
    }


}
