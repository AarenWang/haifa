package org.wrj.allspring.version4.aop.pureannotation;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wrj.allspring.version4.aop.pureannotation.PayService;

public class PayServiceTest {

	
	public static void main(String[] args) {

		ApplicationContext ac = new ClassPathXmlApplicationContext(
				"spring/aop/pureannotation/spring-config-pureannotation.xml");
		PayService ps = ac.getBean("payService", PayService.class);
		ps.payOrder("ZSYH0001", "20161105P01");
		//ps.payOrdreForException();
		
		//ps.payOrdreForException();
	}
}
