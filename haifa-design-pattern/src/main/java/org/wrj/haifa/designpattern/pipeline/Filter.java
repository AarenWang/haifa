package org.wrj.haifa.designpattern.pipeline;

/**
 * 2. 过滤器接口
 */
public interface Filter {
    void execute(UserRegistrationContext context);
}
