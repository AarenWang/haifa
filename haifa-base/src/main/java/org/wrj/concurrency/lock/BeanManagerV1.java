package org.wrj.concurrency.lock;

import java.util.HashMap;
import java.util.Map;

public class BeanManagerV1 {
	private Map<String, Object> map = new HashMap<String, Object>();
	
	public Object getBean(String key)
	{
		synchronized (map) {
			Object bean = map.get(key);
			if(bean == null)
			{
				map.put(key, createBean());
				bean = map.get(key);
			}
			return bean;
		}
	}
	
	public Object createBean()
	{
		//
		return new Object();
	}

}
