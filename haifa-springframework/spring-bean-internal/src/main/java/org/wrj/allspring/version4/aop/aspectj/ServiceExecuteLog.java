package org.wrj.allspring.version4.aop.aspectj;

import java.util.Arrays;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ServiceExecuteLog  {

	public Object invoke(MethodInvocation invocation) throws Throwable {
		String className = invocation.getClass().getName();
		String methodName = invocation.getMethod().getName();
		Object[] args = invocation.getArguments();
		long runBegin = System.currentTimeMillis();
		Object result = invocation.proceed();
		long runEnd = System.currentTimeMillis();
		System.out.printf("Run Class %s %s Argument %s Cost %s millsecond(s) %n ",
				className, methodName, Arrays.toString(args),
				(runEnd - runBegin));
		return result;
	}

}
