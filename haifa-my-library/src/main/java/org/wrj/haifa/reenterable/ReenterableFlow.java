package org.wrj.haifa.reenterable;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by wangrenjun on 2017/9/22.
 */
@Target(ElementType.METHOD)
public @interface ReenterableFlow {

    public String flowName() default  "";


}
