package org.wrj.haifa.retrylog;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Created by wangrenjun on 2017/9/24.
 */
@Aspect
@Component
public class RetryLogAspect {

    // @AfterThrowing(value = "execution(* me.wrj.haifa.retrylog.*.*(..))", throwing = "e")
    public void afterThrow(JoinPoint joinPoint, Throwable e) {
        String className = joinPoint.getTarget().getClass().getName();
        Object[] args = joinPoint.getArgs();
        String kind = joinPoint.getKind();

        System.out.println("============" + e.getClass());
        // ConditionOnException[] exceptions = method.getAnnotationsByType(ConditionOnException.class);
        // if(exceptions != null && exceptions.length > 0){
        // ConditionOnException exceptionCondiont = exceptions[0];
        // Class<?> exceptClass = exceptionCondiont.exceptionClass();
        // if(e.getClass().isAssignableFrom(exceptClass)){
        // System.out.printf("target = %s, method = %s,args=%s,exception class =
        // %",target.getClass().getName(),method.getName(),args,e.getClass().getName());
        // }
        // }

    }

   // @AfterThrowing(value = "execution(* me.wrj.haifa.retrylog.*.*(..))", throwing = "e")
    public void afterThrow(Method method, Object[] args, Object target, Throwable e) {
        ConditionOnException[] exceptions = method.getAnnotationsByType(ConditionOnException.class);
        if (exceptions != null && exceptions.length > 0) {
            ConditionOnException exceptionCondiont = exceptions[0];
            Class<?> exceptClass = exceptionCondiont.exceptionClass();
            if (e.getClass().isAssignableFrom(exceptClass)) {
                System.out.printf("target = %s, method = %s,args=%s,exception class = % \r\n", target.getClass().getName(),
                                  method.getName(), args, e.getClass().getName());
            }
        }
    }

    @AfterReturning(pointcut = "execution(* me.wrj.haifa.retrylog.*.*(..))",returning="returnValue")
    public void afterReturing(JoinPoint joinPoint, Object returnValue) {

        String className = joinPoint.getTarget().getClass().getName();
        Object[] args = joinPoint.getArgs();
        String signName = joinPoint.getSignature().getName();

        System.out.printf("className = %s, args=%s,signName=%s,returnValue=%s \r\n", className, StringUtils.join(args, ","),
                           signName.toString(), ObjectUtils.toString(returnValue));
    }

}
