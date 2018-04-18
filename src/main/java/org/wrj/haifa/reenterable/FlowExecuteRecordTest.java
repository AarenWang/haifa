package org.wrj.haifa.reenterable;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;

/**
 * Created by wangrenjun on 2017/9/22.
 */
public class FlowExecuteRecordTest {

    public static void main(String[] args) {
        MyComplexService myComplexService = new MyComplexServiceImpl();
        FlowExecuteRecordHandler handler = new FlowExecuteRecordHandler(myComplexService);
        //LoggerInterceptor handler = new LoggerInterceptor(myComplexService);
        // MyComplexService myComplexServiceProxy = (MyComplexService)handler.getProxy();
        MyComplexService myComplexServiceProxy = (MyComplexService) Proxy.newProxyInstance(myComplexService.getClass().getClassLoader(),
                myComplexService.getClass().getInterfaces(), handler);

        String purchaseId = "P001";
        Purchase p = new Purchase(purchaseId, "iPhone X", new BigDecimal(8788));
        myComplexServiceProxy.refund(purchaseId, p);

    }
}
