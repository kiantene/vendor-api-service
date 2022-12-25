package com.nextgen.gameaggregator.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity {
    @Column(name = "create_by_id", nullable = false)
    protected Integer createById;
    @Column(name = "create_by_usertype", nullable = false)
    protected String createByUsertype;
    @Column(name = "create_by_ip", nullable = false)
    protected String createByIp;
    @Column(name = "create_date", nullable = false)
    protected Long createDate;

    public Integer getCreateById() {
        return this.createById;
    }
    public String getCreateByUsertype() {
        return this.createByUsertype;
    }
    public String getCreateByIp() {
        return this.createByIp;
    }
    public Long getCreateDate() {
        return this.createDate;
    }

    public void prepareSave(Integer userId, String userType, String currentIp) {
        this.createById = userId;
        this.createByUsertype = userType;
        this.createByIp = currentIp;
        this.createDate = System.currentTimeMillis();
    }

    public String toString() {
        return "created=(" + this.createById + "|" + this.createByUsertype + "|" + this.createByIp + "|" + this.createDate + ")";
    }
}
