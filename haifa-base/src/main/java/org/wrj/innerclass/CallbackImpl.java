package org.wrj.innerclass;

public class CallbackImpl implements ICallback {

	@Override
	public void execute() {
		System.out.println("getCanonicalName():"+this.getClass().getCanonicalName());
		System.out.println("getName():"+this.getClass().getName());
		System.out.println("getSimpleName():"+this.getClass().getSimpleName());

	}

}
