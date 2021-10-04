package org.wrj.allspring.version4.aop.classics;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wrj.allspring.version4.aop.classics.PayService;

public class PayServiceTest {

	public static void main(String[] args) {

		ApplicationContext ac = new ClassPathXmlApplicationContext(
				"spring/aop/classics/spring-*.xml");
		PayService ps = ac.getBean("payService", PayService.class);
		ps.payOrder("ZSYH0001", "20161105P01");
	}

}
