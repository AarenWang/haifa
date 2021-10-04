package org.wrj.concurrency.hashmap;

import java.util.Map;
import java.util.concurrent.Callable;

public class TestSpeed {

}

class CompareTask implements Callable<Long>{
	
	private Map<String,String> map;
	
	public Long call(){
		long begin = System.currentTimeMillis();
		
		
		
		return System.currentTimeMillis() - begin;
		
		
	}
}

