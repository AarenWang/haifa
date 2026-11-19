package org.wrj.allspring.version4.rpc;

import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * RPC 客户端动态代理拦截器。
 * <p>
 * 本示例中，若 Spring 容器内已存在该接口的真实实现（服务端），则直接转发调用；
 * 否则返回默认值，模拟远程调用结果。
 */
public class RpcClientProxy implements InvocationHandler {

    private final Class<?> interfaceClass;
    private final ApplicationContext applicationContext;

    public RpcClientProxy(Class<?> interfaceClass, ApplicationContext applicationContext) {
        this.interfaceClass = interfaceClass;
        this.applicationContext = applicationContext;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        System.out.printf("[MyRpcClient] interface: %s, method: %s, args: %s%n",
                interfaceClass.getName(), method.getName(), args == null ? "[]" : java.util.Arrays.toString(args));

        // 优先转发到容器中的真实实现（模拟 Server 端已启动的场景）
        Object target = findLocalImplementation();
        if (target != null) {
            System.out.println("[MyRpcClient] forwarding to local server implementation: " + target.getClass().getName());
            return method.invoke(target, args);
        }

        // 没有服务端实现时，返回默认值，模拟远程 RPC 调用结果
        System.out.println("[MyRpcClient] no server implementation found, returning default value");
        return defaultValue(method.getReturnType());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object findLocalImplementation() {
        if (applicationContext == null) {
            return null;
        }
        Map beans = applicationContext.getBeansOfType(interfaceClass);
        for (Object bean : beans.values()) {
            if (!(bean instanceof MyRpcClientFactoryBean) && !Proxy.isProxyClass(bean.getClass())) {
                return bean;
            }
        }
        return null;
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        }
        if (returnType == byte.class || returnType == Byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class || returnType == Short.class) {
            return (short) 0;
        }
        if (returnType == int.class || returnType == Integer.class) {
            return 0;
        }
        if (returnType == long.class || returnType == Long.class) {
            return 0L;
        }
        if (returnType == float.class || returnType == Float.class) {
            return 0.0f;
        }
        if (returnType == double.class || returnType == Double.class) {
            return 0.0d;
        }
        if (returnType == char.class || returnType == Character.class) {
            return '\u0000';
        }
        return null;
    }
}
