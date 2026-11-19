package org.wrj.allspring.version4.rpc;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启 {@link MyRpcClient} 扫描，行为类似 MyBatis 的 {@code @MapperScan} 或 Feign 的 {@code @EnableFeignClients}。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MyRpcClientRegistrar.class)
public @interface EnableMyRpcClients {

    /**
     * 扫描的基础包路径。未指定时，默认扫描使用该注解的配置类所在包。
     */
    String[] basePackages() default {};
}
