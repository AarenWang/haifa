package org.wrj.allspring.version4.aop.aopconfig;



public class PayServiceImpl implements PayService {

	public boolean payOrder(String bankAccountId,String orderId) {
		System.out.println(String.format("paying orderId=%s bankAccountId =%s",
				orderId, bankAccountId));

		boolean isPaySuccess = true;
		return isPaySuccess;
	}

	public void payOrdreForException() {
		throw new RuntimeException("343433");
		
	}

}
