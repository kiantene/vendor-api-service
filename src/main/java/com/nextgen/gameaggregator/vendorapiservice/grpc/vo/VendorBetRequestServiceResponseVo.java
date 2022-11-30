package com.nextgen.gameaggregator.vendorapiservice.grpc.vo;

public class VendorBetRequestServiceResponseVo {

    private String transactionId;
    private String currency;
    private Double cash;
    private Double bonus;
    private Double usedPromo;
    private Integer error;
    private String description;

    public VendorBetRequestServiceResponseVo() {
    }

    public VendorBetRequestServiceResponseVo(String transactionId, String currency, Double cash, Double bonus, Double usedPromo, Integer error, String description) {
        this.transactionId = transactionId;
        this.currency = currency;
        this.cash = cash;
        this.bonus = bonus;
        this.usedPromo = usedPromo;
        this.error = error;
        this.description = description;
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

    public Double getCash() {
        return cash;
    }

    public void setCash(Double cash) {
        this.cash = cash;
    }

    public Double getBonus() {
        return bonus;
    }

    public void setBonus(Double bonus) {
        this.bonus = bonus;
    }

    public Double getUsedPromo() {
        return usedPromo;
    }

    public void setUsedPromo(Double usedPromo) {
        this.usedPromo = usedPromo;
    }

    public Integer getError() {
        return error;
    }

    public void setError(Integer error) {
        this.error = error;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
