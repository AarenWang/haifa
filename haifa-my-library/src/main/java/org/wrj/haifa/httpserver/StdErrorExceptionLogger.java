package org.wrj.haifa.httpserver;

import org.apache.http.ExceptionLogger;

/**
 * Created by wangrenjun on 2017/9/26.
 */
public class StdErrorExceptionLogger implements ExceptionLogger {

    @Override
    public void log(Exception ex) {
        System.out.println(ex);
    }
}
