package com.nextgen.gameaggregator.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

    public void prepareSave(Integer userId, String userType) {
        String ip = "Unknown";
        try {
            // This exception should not block the saving of new records
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch(UnknownHostException unknownHostException) {
            unknownHostException.printStackTrace();
        }

        this.createById = userId;
        this.createByUsertype = userType;
        this.createByIp = ip;
        this.createDate = System.currentTimeMillis();
    }

    public String toString() {
        return "created=(" + this.createById + "|" + this.createByUsertype + "|" + this.createByIp + "|" + this.createDate + ")";
    }
}
