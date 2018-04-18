package org.wrj.haifa.retrylog;

/**
 * Created by wangrenjun on 2017/9/24.
 * 当运行方法抛出异常时记录日志
 */
public @interface ConditionOnException {

    /**
     * 默认所有异常都回记录，可以指定特定类型异常才做记录
     * @return
     */
    Class<?>  exceptionClass() default  Throwable.class;

    /**
     * 排除特定异常
     * @return
     */
    Class<?>  excludeClass() default RuntimeException.class;

}
