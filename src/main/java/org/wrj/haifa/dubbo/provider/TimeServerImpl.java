package org.wrj.haifa.dubbo.provider;

import com.alibaba.dubbo.rpc.RpcContext;
import org.wrj.haifa.dubbo.api.DubboConstant;
import org.wrj.haifa.dubbo.api.RemoteInvokeException;
import org.wrj.haifa.dubbo.api.TimeService;

import java.sql.Timestamp;

/**
 * Created by wangrenjun on 2018/4/19.
 */
public class TimeServerImpl implements TimeService{

    @Override
    public Timestamp getCurrentTime() {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    public String getSomething() throws RemoteInvokeException {
//        try {
//            Thread.sleep(1000L);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        throw  new RemoteInvokeException("RemoteInvokeException");
    }

    @Override
    public void invokeWithContxtInfo() {

        String clientThreadName = RpcContext.getContext().getAttachments().get(DubboConstant.DUBBO_CLIENT_THREAD_NAME);
        System.out.println("consumer thread name ="+clientThreadName);
    }
}
