package org.wrj.allspring.version4.tx.entity;

import java.io.Serializable;

/**
 * Created by wangrenjun on 2017/9/19.
 */
public class Author implements Serializable {

    private String id;

    private String nickName;

    private String trueName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getTrueName() {
        return trueName;
    }

    public void setTrueName(String trueName) {
        this.trueName = trueName;
    }
}
