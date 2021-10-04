package org.wrj.innerclass;

public class CallbackContext {
	
	
	public static void main(String[] args) {
		
		ICallback callback = new CallbackImpl();
		callback.execute();
		
		
		ICallback callback2 = new ICallback() {
			
			@Override
			public void execute() {
				System.out.println("getCanonicalName():"+this.getClass().getCanonicalName());
				System.out.println("getName():"+this.getClass().getName());
				System.out.println("getSimpleName():"+this.getClass().getSimpleName());
				
			}
		};
		
		callback2.execute();
		
	}

}
