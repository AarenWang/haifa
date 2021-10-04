package org.wrj.haifa.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.exception.HystrixTimeoutException;


/**
 * Created by wangrenjun on 2017/5/12.
 */
public class SempHelloWorldCommand extends HystrixCommand<String> {

    private final String name;

    public SempHelloWorldCommand(String name){
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("SempHelloWorldCommandGroup"))
                    /* 配置信号量隔离方式,默认采用线程池隔离 */
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)).andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(3)));
        this.name = name;
    }

    @Override
    protected String run() throws Exception {
        // return "SempHelloWorldCommand:" + Thread.currentThread().getName();

        throw new HystrixTimeoutException();

        // TimeUnit.MILLISECONDS.sleep(2000);
        // return name;
    }

    @Override
    protected String getFallback() {
        return "fallback: " + name;
    }

    public static void main(String[] args) throws Exception {
        SempHelloWorldCommand command = new SempHelloWorldCommand("semaphore");
        String result = command.execute();
        System.out.println(result);
        System.out.println("MainThread:" + Thread.currentThread().getName());
    }
}
