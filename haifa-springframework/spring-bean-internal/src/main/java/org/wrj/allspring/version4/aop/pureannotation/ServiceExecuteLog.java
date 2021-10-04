package org.wrj.allspring.version4.aop.pureannotation;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

@Aspect
@Component
public class ServiceExecuteLog {

	
	//@After("execution(* org.wrj.allspring.version4.aop.pureannotation.PayService.payOrder(..))")
	@After("execution (public * *(..))")
	public void afterExecute(JoinPoint jp){
		System.out.println("afterExecute payOrder()");
	}
	
	
	@AfterReturning("execution (public * *(..))")
	public void afterRetrning(JoinPoint jp){
		System.out.println("afterRetrning payOrder()");
		
	}
	
	
	@Before("execution(* org.wrj.allspring.version4.aop.pureannotation.PayService.payOrder(..))")
	public void beforeExecute(JoinPoint jp){
		System.out.println("beforeExecute payOrder()");
	}
	
	@Around("execution(* org.wrj.allspring.version4.aop.pureannotation.PayService.payOrder(..))")
	public Object around(ProceedingJoinPoint jp) throws Throwable{
		Object[] args = jp.getArgs();

		Annotation[] annotations = jp.getTarget().getClass().getDeclaredAnnotations();
		for (Annotation annotation : annotations) {
			System.out.println(annotation);
		}
		//Deprecated deprecated = jp.getTarget().getClass().getDeclaredAnnotation(Deprecated.class);

		//Override override = jp.getTarget().getClass().getDeclaredAnnotation(Override.class);

		System.out.println("around payOrder() before execute,args="+StringUtils.join(args,",") );
		
		Object obj = jp.proceed(args);
		System.out.println("around payOrder() after execute ,result="+obj);
		return obj;
	}
	
	@AfterThrowing(value="execution(* org.wrj.allspring.version4.aop.pureannotation.PayService.payOrdreForException(..))",throwing = "thr")
	public void afterThrowing(JoinPoint jp,Throwable thr){
		System.out.println("afterThrowing payOrdreForException()");
	}
	
	
}
