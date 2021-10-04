package org.wrj.allspring.version4.aop.aopconfig;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

public class ServiceExecuteLog {

	public void beforeInvoke(JoinPoint mi) {
		String className = mi.getTarget().getClass().getName();
		String methodName = mi.getSignature().getName();
		Object[] args = mi.getArgs();

		System.out.printf("will run Class %s %s  args = %s %n ", className,
				methodName, Arrays.toString(args));
	}

	// public Object invoke(MethodInvocation invocation) throws Throwable {
	// String className = invocation.getClass().getName();
	// String methodName = invocation.getMethod().getName();
	// Object[] args = invocation.getArguments();
	// long runBegin = System.currentTimeMillis();
	// Object result = invocation.proceed();
	// long runEnd = System.currentTimeMillis();
	// System.out.printf("Run Class %s %s Argument %s Cost %s millsecond(s) %n ",
	// className, methodName, Arrays.toString(args),
	// (runEnd - runBegin));
	// return result;
	// }

	public Object invoke(ProceedingJoinPoint jp) {
		String className = jp.getTarget().getClass().getName();
		String methodName = jp.getSignature().getName();
		Object[] args = jp.getArgs();
		long runBegin = System.currentTimeMillis();
		Object result = null;
		try {
			result = jp.proceed();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long runEnd = System.currentTimeMillis();
		System.out.printf(
				"Run Class %s %s Argument %s Cost %s millsecond(s) %n ",
				className, methodName, Arrays.toString(args),
				(runEnd - runBegin));
		return result;

	}

	public void processThrowing1(Exception e) throws Throwable{
		
		if(e instanceof RuntimeException){
			throw e;
		}
		System.out.println("-------------------processThrowing1 e.getMessage()---------------------"+e.getMessage());
	}
	
	
	public void processThrowing2(Method method, Object[] args, Object target, Exception e) {
		System.out.println("-------------------processThrowing2 e.getMessage()---------------------"+e.getMessage());
		if(e instanceof RuntimeException){
			throw new IllegalArgumentException();
		}
	}
}
