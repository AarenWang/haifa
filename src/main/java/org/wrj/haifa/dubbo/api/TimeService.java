package org.wrj.haifa.dubbo.api;

import java.sql.Timestamp;

/**
 * Created by wangrenjun on 2018/4/19.
 */
public interface TimeService {

    Timestamp getCurrentTime();

    String getSomething() throws RemoteInvokeException;

    void invokeWithContxtInfo();
}
