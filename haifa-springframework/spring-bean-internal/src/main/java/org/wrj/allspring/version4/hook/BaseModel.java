package org.wrj.allspring.version4.hook;

import java.sql.Timestamp;

/**
 * Created by wangrenjun on 2017/3/14.
 */
public class BaseModel {

    private Timestamp createTime;

    private Timestamp updateTime;

    private Integer version;

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
