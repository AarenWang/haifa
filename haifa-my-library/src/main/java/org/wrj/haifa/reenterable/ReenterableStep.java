package org.wrj.haifa.reenterable;

/**
 * Created by wangrenjun on 2017/9/22.
 */
public @interface ReenterableStep {

    public String flowName();

    public int order() default  1;

    public String stepName() default  "";



}
