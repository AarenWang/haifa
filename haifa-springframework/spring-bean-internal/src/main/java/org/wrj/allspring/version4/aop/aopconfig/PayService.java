package org.wrj.allspring.version4.aop.aopconfig;
public interface PayService {

	public boolean payOrder(String bankAccountId,String orderId);
	
	public void payOrdreForException();
}
