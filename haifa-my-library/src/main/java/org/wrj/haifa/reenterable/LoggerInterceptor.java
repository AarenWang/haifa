package org.wrj.haifa.reenterable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by wangrenjun on 2017/9/24.
 */
public class LoggerInterceptor implements InvocationHandler {

    private Object target;// 目标对象的引用，这里设计成Object类型，更具通用性

    public LoggerInterceptor(Object target){
        this.target = target;
    }

    public Object invoke(Object proxy, Method method, Object[] arg) throws Throwable {
        System.out.println("Entered " + target.getClass().getName() + "-" + method.getName() + ",with arguments{"
                           + arg[0] + "}");
        Object result = method.invoke(target, arg);// 调用目标对象的方法
        System.out.println("Before return:" + result);
        return result;
    }
}
