package org.wrj.allspring.version4.aop.pureannotation;
public interface PayService {

	public boolean payOrder(String bankAccountId,String orderId);
	
	public void payOrdreForException();
}
