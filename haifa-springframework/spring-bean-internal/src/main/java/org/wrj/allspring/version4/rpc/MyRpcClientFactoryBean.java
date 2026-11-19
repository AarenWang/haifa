package org.wrj.allspring.version4.rpc;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Proxy;

/**
 * 为标记了 {@link MyRpcClient} 的接口生成 JDK 动态代理，并作为 Spring Bean 暴露。
 *
 * @param <T> RPC 客户端接口类型
 */
public class MyRpcClientFactoryBean<T> implements FactoryBean<T>, ApplicationContextAware {

    private Class<T> interfaceClass;
    private ApplicationContext applicationContext;

    /**
     * 由 {@link MyRpcClientRegistrar} 通过 BeanDefinition 注入接口类型。
     */
    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() throws Exception {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RpcClientProxy(interfaceClass, applicationContext)
        );
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
